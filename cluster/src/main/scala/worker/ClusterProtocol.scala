package worker

import akka.actor.ActorRef
import uci.MultiInfo
import scala.concurrent.duration.Duration

object MasterProtocol
{
  case class Work(workId: String, job: Any)
  case class WorkResult(workId: String, result: Any)
  case class Ack(workId: String)
}

/**
 * Created by jure on 8/3/14.
 */
object AnalyserProtocol {

  // JOB
  case class AnalyseJob (producer : ActorRef)

  // Producer - Executor commands
  case class WorkerAssigned (worker : ActorRef)
  case class Start (fen: String, skill: String, duration: Duration)
  case class Stop()
  case class Finished()
  case class Ack()

  // Results

  case class Result (evaluation: Float, move: String, line: String)
  case class ResultUci (info: MultiInfo)
  case class BestMove (move: String)
}

object MasterWorkerProtocol {
  // Messages from Workers
  case class RegisterWorker(workerId: String)
  case class WorkerRequestsWork(workerId: String)
  case class WorkIsDone(workerId: String, workId: String, result: Any)
  case class WorkFailed(workerId: String, workId: String)

  // Messages to Workers
  case object WorkIsReady
  case class Ack(id: String)
}

