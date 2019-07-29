package akka.frp.tests

import akka.actor._
import akka.frp._
import utest._
import akka.testkit._
import scala.util.continuations.reset

object RequestResponseTests extends TestSuite with TestKitBase {
  implicit lazy val system = ActorSystem()

  object TestRequestDef extends RequestDefinition[String, String]

  class TestServer extends ChannelActor with RequestServerActor {
    handleRequest(TestRequestDef){ rq =>
      s"resp $rq"
    }
  }

  object TestChannelDef extends PersistentChannelDefinition[String]

  class TestClient(server: ActorRef, testActor: ActorRef) extends ChannelActor with RequestClientActor with PersistentChannelHandlerActor {
    handleInput(TestChannelDef) { rq =>
      reset {
        testActor ! rq
        val response = getOne(TestRequestDef, server)(rq)
        testActor ! response
      }
    }
  }

  val tests = Tests {
    test("request/response") {
      val server = system.actorOf(Props(new TestServer))
      val client = system.actorOf(Props(new TestClient(server, testActor)))

      TestChannelDef.send(client, "asd")
      expectMsg("asd")
      expectMsg("resp asd")
      ()
    }
  }
}
