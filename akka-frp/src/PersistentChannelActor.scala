package akka.frp

import akka.actor.ActorRef
import ChannelActor._

trait PersistentChannelDefinition[T] extends PersistentChannelId {
  def channelOf(a: ActorRef): ChannelInput[T] = ChannelInput[T](a, this)
  def send(to: ActorRef, msg: T): Unit = channelOf(to) ! msg
}

trait PersistentChannelHandlerActor { _: ChannelActor =>
  protected def openChannelFor[T](cd: PersistentChannelDefinition[T]): Channel[T] = {
    openPersistentChannel[T](cd)
  }
  protected def handleInput[T](cd: PersistentChannelDefinition[T])(handler: T => Unit): Unit = {
    openChannelFor[T](cd).output.events.foreach(handler)
  }
}
