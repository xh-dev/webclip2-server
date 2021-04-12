package app.actor

import java.security.SecureRandom
import java.util.Date

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.pattern.StatusReply
import dev.xethh.utils.BinarySizeUtils.BinarySize
import me.xethh.utils.dateUtils.D

import scala.beans.BeanProperty
import scala.concurrent.duration.{Duration, _}

object WebClip2Actor {

  def apply(config: WebClip2Config): Behavior[WebClip2Cmd] = {
    Behaviors.setup { context =>
      val DEFAULT_MAX_POOL = 1000000
      val (maxVol, msgMaxSize, maxPool, duration) = Option(config)
        .map { it =>
          val WebClip2Config(inMaxVol, inMaxMsgSize, inMaxPoolSize, duration) = it
          (inMaxVol.inBytes().longValue(), inMaxMsgSize.inBytes().longValue(), if (inMaxPoolSize <= 0 || inMaxPoolSize >= DEFAULT_MAX_POOL) DEFAULT_MAX_POOL - 1 else inMaxPoolSize, duration)
        }
        .getOrElse((BinarySize.ofGigaByte(1L).inBytes().longValue(), BinarySize.ofMegaByte(1L).inBytes().longValue(), DEFAULT_MAX_POOL - 1, 30.minute))

      val random = new SecureRandom()
      var cur = 0L
      var map: Map[Int, (Date, String)] = Map.empty[Int, (Date, String)]

      def add(msg: String): String = {
        context.log.info(s"start add msg")

        def checkVol(msg: String): Boolean = msg.length + cur > maxVol

        def checkMsgSize(msg: String): Boolean = msg.length > msgMaxSize

        def checkPool(): Boolean = map.size == maxPool - 1

        def getNext(): Int = {
          val next = random.nextInt(maxPool - 1) + 1
          if (map.contains(next)) getNext() else next
        }

        if (checkVol(msg)) {
          throw new RuntimeException("Storage currently full!")
        }
        if (checkMsgSize(msg)) {
          throw new RuntimeException("Msg larger than max size!")
        }
        if (checkPool()) {
          throw new RuntimeException("Pool currently full!")
        }

        val next = getNext()
        context.log.info(s"next id: ${next}")

        map += (next -> (new java.util.Date(), msg))
        cur += msg.length

        context.log.info(s"complete adding msg: ${next}")
        "%06d".format(next)
      }

      def retrieve(str: String) = {
        context.log.info(s"start retrieve ${str}")

        def isNumber(str: String) = try {
          str.toInt
          true
        }
        catch {
          case t: Throwable =>
            false
        }

        def checkId(int: Int): Boolean = int >= maxPool || int <= 0

        if (!isNumber(str))
          throw new RuntimeException(s"[${str}] does not valid")
        val int = str.toInt
        if (checkId(int))
          throw new RuntimeException(s"[${str}] does not in range")

        if (!map.contains(int))
          throw new RuntimeException(s"[${str}] is empty")

        val msg = map(int)
        map -= int
        cur -= msg._2.length

        context.log.info(s"complete retrieve ${str}")
        msg._2
      }

      def refresh() = {
        context.log.info("Refresh start")
        val msgToPurge = map.filter { it =>
          D.dt().now().addMS((duration.toMillis * -1L).toInt).laterThan(it._2._1)
        }

        msgToPurge.foreach { it =>
          context.log.info(s"purge msg: ${it._1}")
          val msg = map(it._1)
          map -= it._1
          cur -= msg._2.length
        }

        context.log.info("Refresh end")
      }

      context.system.scheduler.scheduleWithFixedDelay(1.second, 1.minute) { () =>
        context.self ! WebClip2RefreshCmd
      }(context.system.executionContext)


      Behaviors.receiveMessage {
        case WebClip2RefreshCmd =>
          refresh()
          Behaviors.same
        case WebClip2ConfigCmd(replyTo) =>
          replyTo ! StatusReply.success(WebClip2Config(BinarySize.ofByte(maxVol), BinarySize.ofByte(msgMaxSize), maxPool, duration))
          Behaviors.same
        case WebClip2StatusCmd(replyTo) =>
          replyTo ! StatusReply.success(WebClip2Status(BinarySize.ofByte(cur), map.size))
          Behaviors.same
        case NewWebClip2Cmd(msg, replyTo) =>
          try {
            val id = add(msg)
            replyTo ! StatusReply.success(id)
          }
          catch {
            case e: Throwable =>
              context.log.error(s"Fail to create message")
              replyTo ! StatusReply.error(e)
          }
          Behaviors.same
        case RetrieveWebClip2Cmd(code, replyTo) =>
          try {
            val msgExtracted = retrieve(code)
            replyTo ! StatusReply.success(msgExtracted)
          }
          catch {
            case e: Throwable =>
              context.log.error(s"Fail to retrieve message: ${code}")
              replyTo ! StatusReply.error(e)
          }
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }

  }

  trait WebClip2Cmd

  case class WebClip2Config(
                             @BeanProperty maxVolume: BinarySize,
                             @BeanProperty msgMaxSize: BinarySize,
                             @BeanProperty maxPoolSize: Int,
                             @BeanProperty timeout: Duration
                           )

  case class WebClip2Status(
                             @BeanProperty currentSize: BinarySize,
                             @BeanProperty slotUsed: Int
                           )

  case class RetrieveWebClip2Cmd(code: String, replyTo: ActorRef[StatusReply[String]]) extends WebClip2Cmd

  case class NewWebClip2Cmd(msg: String, replyTo: ActorRef[StatusReply[String]]) extends WebClip2Cmd

  case class WebClip2ConfigCmd(replyTo: ActorRef[StatusReply[WebClip2Config]]) extends WebClip2Cmd

  case class WebClip2StatusCmd(replyTo: ActorRef[StatusReply[WebClip2Status]]) extends WebClip2Cmd

  protected[this] case object WebClip2RefreshCmd extends WebClip2Cmd


}
