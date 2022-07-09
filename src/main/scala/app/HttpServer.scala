package app

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.{Http, server}
import akka.pattern.StatusReply
import akka.util.Timeout
import app.actor.WebClip2Actor._
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import dev.xethh.utils.binarySizeUtilsJacksonExtension.Module

import scala.beans.BeanProperty
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success}

object HttpServer {

  val om: ObjectMapper = Module.inject(new ObjectMapper() with ScalaObjectMapper).registerModule(DefaultScalaModule)

  implicit def anyToJson[A](a: A): String = om.writeValueAsString(a)

  def apply(system: ActorSystem[Nothing], actor: ActorRef[WebClip2Cmd]): Unit = {

    implicit val sys: ActorSystem[Nothing] = system

    implicit val ec: ExecutionContextExecutor = system.executionContext
    implicit val duration: FiniteDuration = 2.minute
    implicit val timeout: Timeout = Timeout(duration)
    implicit val scheduler: Scheduler = system.scheduler

    import ch.megard.akka.http.cors.scaladsl.CorsDirectives._


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
              entity(as[String]) { str: String =>
                val post = Option(om.readValue[PostReq](str, new TypeReference[PostReq] {}))
                  .filter(_.msg != null)

                if (post.isEmpty)
                  complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, anyToJson(ErrorResponse("Empty msg"))))
                else {
                  onCompleteTask[String](
                    actor.ask[StatusReply[String]](ref => NewWebClip2Cmd(post.get.msg, ref))(timeout, scheduler),
                    it => complete(HttpEntity(ContentTypes.`application/json`, "Test: " + anyToJson(StringResponse(it))))
                  )
                }
              }
            }
          }
        }
      )
    }

    Http().newServerAt("0.0.0.0", 8080).bind(route)

    //val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    //
    //println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    //StdIn.readLine() // let it run until user presses return
    //bindingFuture
    //  .flatMap(_.unbind()) // trigger unbinding from the port
    //  .onComplete(_ => system.terminate()) // and shutdown when done
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
    def this()={
      this("","","")
    }

  }
}
