package worker

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator
import scala.concurrent.duration.Deadline
import scala.concurrent.duration.FiniteDuration
import akka.actor.Props
import akka.contrib.pattern.ClusterReceptionistExtension

import worker.MasterProtocol._

object Master {

  val ResultsTopic = "results"

  def props(workTimeout: FiniteDuration): Props =
    Props(classOf[Master], workTimeout)

  private sealed trait WorkerStatus
  private case object Idle extends WorkerStatus
  private case class Busy(workId: String, deadline: Deadline) extends WorkerStatus
  private case class WorkerState(ref: ActorRef, status: WorkerStatus)

  private case object CleanupTick

}

class Master(workTimeout: FiniteDuration) extends Actor with ActorLogging {
  import Master._
  import WorkState._

  val mediator = DistributedPubSubExtension(context.system).mediator
  ClusterReceptionistExtension(context.system).registerService(self)

  // workers state is not event sourced
  private var workers = Map[String, WorkerState]()

  // workState can be event sourced
  private var workState = WorkState.empty

  import context.dispatcher
  val cleanupTask = context.system.scheduler.schedule(workTimeout / 2, workTimeout / 2,
    self, CleanupTick)

  override def postStop(): Unit = cleanupTask.cancel()

  override def receive: Receive = {
    case MasterWorkerProtocol.RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        log.info("Worker registered: {}", workerId)
        workers += (workerId -> WorkerState(sender(), status = Idle))
        if (workState.hasWork)
          sender() ! MasterWorkerProtocol.WorkIsReady
      }

    case MasterWorkerProtocol.WorkerRequestsWork(workerId) =>
      if (workState.hasWork) {
        workers.get(workerId) match {
          case Some(s @ WorkerState(_, Idle)) =>
            val work = workState.nextWork
            workState = workState.updated (WorkStarted(work.workId))
            log.info("Giving worker {} some work {}", workerId, work.workId)
            workers += (workerId -> s.copy(status = Busy(work.workId, Deadline.now + workTimeout)))
            sender() ! work
          case _ =>
        }
      }

    case MasterWorkerProtocol.WorkIsDone(workerId, workId, result) =>
      // idempotent
      if (workState.isDone(workId)) {
        // previous Ack was lost, confirm again that this is done
        sender() ! MasterWorkerProtocol.Ack(workId)
      } else if (!workState.isInProgress(workId)) {
        log.info("Work {} not in progress, reported as done by worker {}. Can be due to master failover.", workId, workerId)

        sender ! MasterWorkerProtocol.Ack(workId)

      } else {
        log.info("Work {} is done by worker {}", workId, workerId)
        changeWorkerToIdle(workerId, workId)
        workState = workState.updated(WorkCompleted(workId, result))
        mediator ! DistributedPubSubMediator.Publish(ResultsTopic, WorkResult(workId, result))
        // Ack back to original sender
        sender ! MasterWorkerProtocol.Ack(workId)
      }

    case MasterWorkerProtocol.WorkFailed(workerId, workId) =>
      if (workState.isInProgress(workId)) {
        log.info("Work {} failed by worker {}", workId, workerId)
        changeWorkerToIdle(workerId, workId)
        workState = workState.updated(WorkerFailed(workId))
        notifyWorkers()
      }

    case work: Work =>
      // idempotent
      if (workState.isAccepted(work.workId)) {
        sender() ! MasterProtocol.Ack(work.workId)
      } else {
        log.info("Accepted work: {}", work.workId)
        // Ack back to original sender
        sender() ! MasterProtocol.Ack(work.workId)
        workState = workState.updated(WorkAccepted(work))
        notifyWorkers()
      }

    case CleanupTick =>
      for ((workerId, s @ WorkerState(_, Busy(workId, timeout))) ← workers) {
        if (timeout.isOverdue) {
          log.info("Work timed out: {}", workId)
          workers -= workerId
          workState = workState.updated(WorkerTimedOut(workId))
          notifyWorkers()
        }
      }
  }

  def notifyWorkers(): Unit =
    if (workState.hasWork) {
      // could pick a few random instead of all
      workers.foreach {
        case (_, WorkerState(ref, Idle)) => ref ! MasterWorkerProtocol.WorkIsReady
        case _                           => // busy
      }
    }

  def changeWorkerToIdle(workerId: String, workId: String): Unit =
    workers.get(workerId) match {
      case Some(s @ WorkerState(_, Busy(`workId`, _))) ⇒
        workers += (workerId -> s.copy(status = Idle))
      case _ ⇒
      // ok, might happen after standby recovery, worker state is not persisted
    }

  // TODO cleanup old workers
  // TODO cleanup old workIds, doneWorkIds

}
