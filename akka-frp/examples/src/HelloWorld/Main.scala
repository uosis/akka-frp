package akka.frp.examples.HelloWorld

import akka.actor._
import akka.frp._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.continuations.reset

object SimpleGreeting extends RequestDefinition[String, String]

object EavesDroppingChannel extends PersistentChannelDefinition[String]

class GreetingsServer(eavesDroppers: Seq[ActorRef]) extends ChannelActor with RequestServerActor {
  handleRequest(SimpleGreeting){ rq =>
    val resp = s"Hello, $rq"
    eavesDroppers.foreach(e => EavesDroppingChannel.send(e, resp))
    resp
  }
}

class EavesDropper extends ChannelActor with PersistentChannelHandlerActor{
  handleInput(EavesDroppingChannel) { msg =>
    println(s"Overheard $msg")
  }
}

class AdvancedEavesDropper extends ChannelActor with PersistentChannelHandlerActor{
  val channel = openChannelFor(EavesDroppingChannel)
  val johnsGreetings = channel.output.events.filter(_.contains("John"))

  reset {
    val firstJohn = johnsGreetings.collectOne {
      case s if s.endsWith("1") => s
    }
    val secondJohn = johnsGreetings.collectOne {
      case s if s.endsWith("2") => s
    }
    println(s"Both Johns were greeted: $firstJohn, $secondJohn")
  }
}

object GreetingRequests extends PersistentChannelDefinition[String]

class GreetingsClient(greetingsServer: ActorRef) extends ChannelActor with RequestClientActor with PersistentChannelHandlerActor {
  handleInput(GreetingRequests) { rq =>
    println(s"Sending greeting request for $rq")
    reset {
      val response = getOne(SimpleGreeting, greetingsServer)(rq)
      println(s"Greeting received: $response")
    }
  }
}

object Main extends App {
  val system = ActorSystem("HelloWorld")

  val ed1 = system.actorOf(Props(new EavesDropper))
  val ed2 = system.actorOf(Props(new AdvancedEavesDropper))
  val server = system.actorOf(Props(new GreetingsServer(Seq(ed1, ed2))))
  val client = system.actorOf(Props(new GreetingsClient(server)))
  
  GreetingRequests.send(client, "Bob")
  GreetingRequests.send(client, "John A 1")
  GreetingRequests.send(client, "Alice")
  GreetingRequests.send(client, "John B 2")

  Thread.sleep(2000)

  Await.ready(system.terminate(), Duration.Inf)
}
