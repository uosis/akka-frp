package akka

import frp.ChannelActor.ChannelInput
import _root_.io.dylemma.frp._

import scala.util.continuations.shift

package object frp {
  implicit class EventStreamImplicits[T](es: EventStream[T]) {
    /**
     * Pipe all events from this event stream to the provided channel.
     */
    def pipe[U >: T](c: ChannelInput[U])(implicit obs: Observer): Unit = es.foreach(c.!)

    /**
     * Pipe first event from this event stream to the provided channel, and close the channel.
     */
    def pipeOnce[U >: T](c: ChannelInput[U])(implicit obs: Observer): Unit = es.onNext(c.!!)

    /**
     * Pipe events from this event stream to the provided channel until `noMore` evaluates to true, and then close the channel.
     */
    def pipeUntil[U >: T](c: ChannelInput[U])(noMore: T => Boolean)(implicit obs: Observer): Unit = {
      es.sink {
        case Fire(e) =>
          c ! e
          if (noMore(e)) {
            c.close()
            false
          } else {
            true
          }
        case Stop =>
          c.close()
          false
      }
    }

    /**
     * Collect the first matching event from this stream. CPS style.
     */
    def collectOne[R](mf: PartialFunction[T, R])(implicit obs: Observer) = {
      shift { k: (R => Unit) =>
        es.collect(mf).onNext(k)
      }
    }

    /**
     * Receive the next event from this stream. CPS style.
     */
    def waitOne()(implicit obs: Observer) = collectOne { case a => a }
  }
}
