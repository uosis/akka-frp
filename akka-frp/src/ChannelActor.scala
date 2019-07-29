package akka.frp

import java.util.UUID
import akka.actor._
import com.typesafe.scalalogging.LazyLogging
import io.dylemma.frp._

/**
 * The main FRP actor base trait.
 * Holds channels and dispatches events.
 */
trait ChannelActor extends Actor with Observer with LazyLogging {
  import ChannelActor._

  private val channels = collection.mutable.Map.empty[ChannelId, EventSource[Any]]

  override def receive: Receive = {
    case msg: ChannelMessage =>
      channels.get(msg.channelId) match {
        case Some(channel) =>
          msg match {
            case ChannelEvent(id, ev) =>
              channel.fire(ev)
            case ChannelSingleEvent(id, ev) =>
              channel.fire(ev)
              channel.stop()
              channels.remove(id)
            case ChannelClose(id) =>
              channel.stop()
              channels.remove(id)
          }
        case None =>
          val d = DeadChannelMessage(msg, sender, self)
          logger.warn(d.toString)
          context.system.eventStream.publish(d)
      }
  }

  private def openChannel[T](id: ChannelId): Channel[T] = {
    val es = channels.getOrElseUpdate(id, EventSource[Any])

    val in = ChannelInput[T](self, id)
    val out = ChannelOutput[T](es.map(_.asInstanceOf[T]))

    Channel(in, out)
  }

  protected def openTransientChannel[T](): Channel[T] = openChannel[T](TransientChannelId(UUID.randomUUID()))
  protected def openPersistentChannel[T](id: PersistentChannelId): Channel[T] = openChannel[T](id)
}

object ChannelActor {
  sealed trait ChannelId

  /**
   * Persistent channel id is expected to be constant throughout the lifecycle of the application.
   */
  trait PersistentChannelId extends ChannelId

  /**
   * Transient channel id is generated each time a channel is opened.
   */
  case class TransientChannelId(id: UUID) extends ChannelId

  case class DeadChannelMessage(message: ChannelMessage, sender: ActorRef, recipient: ActorRef)

  sealed trait ChannelMessage {
    def channelId: ChannelId
  }
  case class ChannelEvent(channelId: ChannelId, event: Any) extends ChannelMessage
  case class ChannelSingleEvent(channelId: ChannelId, event: Any) extends ChannelMessage
  case class ChannelClose(channelId: ChannelId) extends ChannelMessage with DeadLetterSuppression

  case class ChannelInput[-T](owner: ActorRef, id: ChannelId) {
    /**
     * Send an event to this channel.
     */
    def !(event: T)(implicit sender: ActorRef = Actor.noSender): Unit = owner ! ChannelEvent(id, event)

    /**
     * Send an event to this channel, and then close the channel.
     */
    def !!(event: T)(implicit sender: ActorRef = Actor.noSender): Unit = owner ! ChannelSingleEvent(id, event)

    def close()(implicit sender: ActorRef = Actor.noSender): Unit = owner ! ChannelClose(id)
  }

  case class ChannelOutput[+T](events: EventStream[T])

  case class Channel[T](input: ChannelInput[T], output: ChannelOutput[T])
}
