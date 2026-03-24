package app

import app.actor.WebClip2Actor.*
import dev.xethh.utils.BinarySizeUtils.BinarySize
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, Scheduler}
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.http.scaladsl.server.StandardRoute
import org.apache.pekko.http.scaladsl.{Http, server}
import org.apache.pekko.pattern.StatusReply
import org.apache.pekko.util.Timeout
import tools.jackson.core.`type`.TypeReference
import tools.jackson.core.{JsonGenerator, JsonParser}
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.StdSerializer
import tools.jackson.databind.{DeserializationContext, ObjectMapper, SerializationContext}
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.module.scala.DefaultScalaModule

import scala.beans.BeanProperty
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

class BinarySerializer extends StdSerializer[BinarySize](classOf[BinarySize]):
  override def serialize(binarySize: BinarySize, gen: JsonGenerator, context: SerializationContext): Unit = {
    if (binarySize == null) {
      gen.writeNull()
    } else {
      gen.writeRawValue(binarySize.inBytes().toBigInteger.longValue().toString);
    }
  }

class BinaryDeserializer extends StdDeserializer[BinarySize](classOf[BinarySize]):
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): BinarySize =
    if ("null".equals(jsonParser.getValueAsString())) { null }
    else { BinarySize.ofByte(jsonParser.getBigIntegerValue.longValue()) }

val om: ObjectMapper = JsonMapper.builder()
  .addModule(DefaultScalaModule)
  .addModule(
    new SimpleModule()
      .addSerializer(new BinarySerializer())
      .addDeserializer(classOf[BinarySize], new BinaryDeserializer())
  ).build()

object HttpServer:

  implicit def anyToJson[A](a: A): String = om.writeValueAsString(a)

  def apply(system: ActorSystem[Nothing], actor: ActorRef[WebClip2Cmd]): Unit = {

    implicit val sys: ActorSystem[Nothing] = system

    implicit val ec: ExecutionContextExecutor = system.executionContext
    implicit val duration: FiniteDuration = 2.minute
    implicit val timeout: Timeout = Timeout(duration)
    implicit val scheduler: Scheduler = system.scheduler

    import org.apache.pekko.http.cors.scaladsl.CorsDirectives.*


    def onCompleteTask[Res](
                             task: => Future[StatusReply[Res]],
                             successOperation: Res => StandardRoute
                           ): server.Route = {
      onComplete(task) {
        case Success(v) =>
          if (v.isSuccess) {
            successOperation(v.getValue)
          } else {
            complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, anyToJson(ErrorResponse(Option(v.getError).map(_.getMessage).getOrElse("Unkown error")))))
          }
        case Failure(exception) =>
          complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`application/json`, anyToJson(ErrorResponse(exception.getMessage))))
      }
    }

    val route = cors() {
      concat(
        path("version") {
          get {
            val om = new ObjectMapper(new YAMLFactory())
            val versionMeta = io.Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream("version.txt")).mkString
            val data = om.readValue(versionMeta, classOf[Version])
            complete(HttpEntity(ContentTypes.`application/json`, anyToJson(data)))
          }
        },
        path("status") {
          get {
            onCompleteTask[WebClip2Status](
              actor.ask[StatusReply[WebClip2Status]](ref => WebClip2StatusCmd(ref))(timeout, scheduler),
              it => complete(HttpEntity(ContentTypes.`application/json`, anyToJson(StatusResponse(it))))
            )
          }
        },
        path("config") {
          get {
            onCompleteTask[WebClip2Config](
              actor.ask[StatusReply[WebClip2Config]](ref => WebClip2ConfigCmd(ref))(timeout, scheduler),
              it => complete(HttpEntity(ContentTypes.`application/json`, anyToJson(ConfigResponse(it))))
            )
          }
        },
        path("msg" / "retrieve") {
          post {
            decodeRequest {
              entity(as[String]) { str =>
                val post = Option(om.readValue[RetrieveReq](str, new TypeReference[RetrieveReq] {}))
                  .filter(_.code != null)

                if (post.isEmpty)
                  complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, anyToJson(ErrorResponse("Empty msg"))))
                else {
                  onCompleteTask[String](
                    actor.ask[StatusReply[String]](ref => RetrieveWebClip2Cmd(post.get.code, ref))(timeout, scheduler),
                    it => complete(HttpEntity(ContentTypes.`application/json`, anyToJson(RetrieveResponse(it))))
                  )
                }
              }
            }
          }
        },
        path("msg" / "create") {
          post {
            decodeRequest {
              entity(as[String]) { (str: String) =>
                val post = Option(om.readValue[PostReq](str, new TypeReference[PostReq] {}))
                  .filter(_.msg != null)

                if (post.isEmpty)
                  complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, anyToJson(ErrorResponse("Empty msg"))))
                else {
                  onCompleteTask[String](
                    actor.ask[StatusReply[String]](ref => NewWebClip2Cmd(post.get.msg, ref))(timeout, scheduler),
                    it => complete(HttpEntity(ContentTypes.`application/json`, anyToJson(StringResponse(it))))
                  )
                }
              }
            }
          }
        }
      )
    }
    Http().newServerAt("0.0.0.0", 8080).bind(route)
  }

  trait Response
  case class PostReq(@BeanProperty msg: String)
  case class RetrieveReq(@BeanProperty code: String)
  case class ErrorResponse[String](@BeanProperty errorMsg: String) extends Response
  case class StringResponse[String](@BeanProperty id: String) extends Response
  case class RetrieveResponse[String](@BeanProperty msg: String) extends Response
  case class StatusResponse(@BeanProperty status: WebClip2Status) extends Response
  implicit def d2Json[A](d: A): String = om.writeValueAsString(d)
  case class ConfigResponse(@BeanProperty status: WebClip2Config) extends Response
  case class VersionResponse(@BeanProperty version: String)
  case class Version(
                      @BeanProperty branch: String,
                      @BeanProperty version: String,
                      @BeanProperty commit: String,
                    ) {
    def this()={this("","","")}
  }
