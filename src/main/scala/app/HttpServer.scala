package app

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.pattern.StatusReply
import akka.util.Timeout
import app.actor.WebClip2Actor._
import ch.megard.akka.http.cors.javadsl.settings.CorsSettings
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import dev.xethh.utils.binarySizeUtilsJacksonExtension.Module

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{DurationInt, FiniteDuration}
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


    val route = {
      cors() {
        path("status") {
          get {
            onComplete(
              actor.ask[StatusReply[WebClip2Status]](ref => WebClip2StatusCmd(ref))(timeout, scheduler)
            ) {
              case Success(v) =>
                if (v.isSuccess) {
                  complete(HttpEntity(ContentTypes.`application/json`, StatusResponse(v.getValue)))
                }
                else {
                  complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse(Option(v.getError).map(_.getMessage).getOrElse("Unkown error"))))
                }
              case Failure(exception) =>
                complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`application/json`, ErrorResponse(exception.getMessage)))
            }
          }
        } ~
          path("config") {
            get {
              onComplete(
                actor.ask[StatusReply[WebClip2Config]](ref => WebClip2ConfigCmd(ref))(timeout, scheduler)
              ) {
                case Success(v) =>
                  if (v.isSuccess) {
                    complete(HttpEntity(ContentTypes.`application/json`, ConfigResponse(v.getValue)))
                  }
                  else {
                    complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse(Option(v.getError).map(_.getMessage).getOrElse("Unkown error"))))
                  }
                case Failure(exception) =>
                  complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`application/json`, ErrorResponse(exception.getMessage)))
              }
            }
          } ~
          path("msg" / "retrieve") {
            post {
              decodeRequest {
                entity(as[String]) { str =>
                  val post = Option(om.readValue[RetrieveReq](str, new TypeReference[RetrieveReq] {}))
                    .filter(_.code != null)

                  if (post.isEmpty)
                    complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse("Empty msg")))
                  else {
                    onComplete(
                      actor.ask[StatusReply[String]](ref => RetrieveWebClip2Cmd(post.get.code, ref))(timeout, scheduler)
                    ) {
                      case Success(v) =>
                        if (v.isSuccess) {
                          complete(HttpEntity(ContentTypes.`application/json`, RetrieveResponse(v.getValue)))
                        }
                        else {
                          complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse(Option(v.getError).map(_.getMessage).getOrElse("Unkown error"))))
                        }
                      case Failure(exception) =>
                        complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse(exception.getMessage)))
                    }
                  }
                }

              }
            }
          } ~
          path("msg" / "create") {
            post {
              decodeRequest {
                entity(as[String]) { str: String =>
                  val post = Option(om.readValue[PostReq](str, new TypeReference[PostReq] {}))
                    .filter(_.msg != null)

                  if (post.isEmpty)
                    complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse("Empty msg")))
                  else {
                    onComplete(
                      actor.ask[StatusReply[String]](ref => NewWebClip2Cmd(post.get.msg, ref))(timeout, scheduler)
                    ) {
                      case Success(v) =>
                        if (v.isSuccess) {
                          complete(HttpEntity(ContentTypes.`application/json`, StringResponse(v.getValue)))
                        }
                        else {
                          complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse(Option(v.getError).map(_.getMessage).getOrElse("Unkown error"))))
                        }
                      case Failure(exception) =>
                        complete(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/html(UTF-8)`, ErrorResponse(exception.getMessage)))
                    }
                  }
                }
              }
            }
          }
      }
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

  case class StatusResponse[String](@BeanProperty status: WebClip2Status) extends Response

  implicit def d2Json[A](d: A) = om.writeValueAsString(d)

  case class ConfigResponse[String](@BeanProperty status: WebClip2Config) extends Response
}
