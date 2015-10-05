package workers

import akka.actor.Actor
import akka.actor.ActorLogging
import worker.MasterProtocol.WorkResult
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator

class WorkResultConsumer extends Actor with ActorLogging {

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe("results", self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }

}