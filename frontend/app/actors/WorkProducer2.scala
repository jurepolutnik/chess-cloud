package worker

import java.util.UUID
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import worker.AnalyserProtocol._
import worker.MasterProtocol.Work


object WorkProducer2 {
  case object Tick
  case object StopTick
}

class WorkProducer2(frontend: ActorRef) extends Actor with ActorLogging {
  import WorkProducer2._
  import context.dispatcher

  def scheduler = context.system.scheduler
  def rnd = ThreadLocalRandom.current
  def nextWorkId(): String = UUID.randomUUID().toString

  var n = 0
  val fen = """rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"""

  override def preStart(): Unit =
    scheduler.scheduleOnce(5.seconds, self, Tick)

  // override postRestart so we don't call preStart and schedule a new Tick
  override def postRestart(reason: Throwable): Unit = ()

  def receive = {
    case Tick =>
      log.info("Produced work: {}", fen)
      val work = Work(nextWorkId(), AnalyseJob(producer = self))
      frontend ! work
      context.become(waitAccepted(work))
  }


  def waitAccepted(work: Work): Actor.Receive = {
    case Frontend.Ok =>
      context.become (waitForWorker)
    case Frontend.NotOk =>
      log.info("Work not accepted, retry after a while")
      scheduler.scheduleOnce(3.seconds, frontend, work)
  }

  def waitForWorker : Receive = {
    case WorkerAssigned (worker : ActorRef) =>
      worker ! Start (fen, "20", 3.seconds)
      scheduler.scheduleOnce(2.seconds, self, StopTick)
      context.become(analysisStarted(worker))

    case _ =>
      log.error("Unknown message in waitAssigned state... Reverting to receive mode.")
      context.become(receive)
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
  }

  def analysisStarted (worker: ActorRef) : Receive = {

    case r: Result =>
      log.info ("Result : "+r)

    case r: ResultUci =>
      log.info ("ResultUci : "+r)

    case StopTick =>
      worker ! Stop

    case f: Finished =>
      log.info ("Finished : "+f)
      context.become (receive)
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)

    case unknown =>
      log.info (s"Unknown message from worker: {}", unknown)
  }


}