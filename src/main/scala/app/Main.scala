package app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import app.actor.WebClip2Actor
import app.actor.WebClip2Actor._
import dev.xethh.utils.BinarySizeUtils.BinarySize

import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    val config = WebClip2Config(BinarySize.ofGigaByte(1L), BinarySize.ofMegaByte(1L), 1000000, 30.minute)
    val system = ActorSystem.create(Behaviors.empty, "system")

    val actor = system.systemActorOf(WebClip2Actor(config), "webclip-actor")

    HttpServer(system, actor)

  }


}
