package akka.frp

import ChannelActor._
import akka.actor.ActorRef

case class Request[RQ, RP](req: RQ, resp: ChannelInput[RP])

trait RequestDefinition[RQ, RP] extends PersistentChannelDefinition[Request[RQ, RP]] {
  type ThisRequest = Request[RQ, RP]

  def sendRequest(to: ActorRef, req: ThisRequest): Unit = send(to, req)
}

trait RequestServerActor extends PersistentChannelHandlerActor { _: ChannelActor =>
  protected def handleRequest[RQ, RP](reqDef: RequestDefinition[RQ, RP])(handler: RQ => RP): Unit = {
    handleInput(reqDef){ rq =>
      rq.resp !! handler(rq.req)
    }
  }
}

trait RequestClientActor { _: ChannelActor =>
  protected def sendRequest[RQ, RP](reqDef: RequestDefinition[RQ, RP], to: ActorRef)(req: RQ): ChannelOutput[RP] = {
    val rc = openTransientChannel[RP]()
    reqDef.sendRequest(to, Request(req, rc.input))
    rc.output
  }
  protected def getOne[RQ, RP](reqDef: RequestDefinition[RQ, RP], to: ActorRef)(req: RQ) = sendRequest(reqDef, to)(req).events.waitOne()
}
