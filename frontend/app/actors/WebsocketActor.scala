package actors

import akka.actor._
import misc.Global
import play.api.libs.iteratee.{Enumerator, Concurrent}

import worker.{Frontend, AnalyserProtocol}
import worker.AnalyserProtocol._
import java.util.UUID
import scala.concurrent.duration._
import worker.MasterProtocol._
import play.api.libs.json.{JsObject, JsString, Json}
import actors.SocketProtocol._
import ClusterActor.{Message}

/**
 * Created with IntelliJ IDEA.
 * User: xlab
 * Date: 28/01/14
 * Time: 22:18
 * To change this template use File | Settings | File Templates.
 */

// Actor messages
object ClusterActor
{
  case class Message(msg: String)

  def propsWebSocket(out: ActorRef) = Props(new WebsocketActor(out))
  def propsChuncked(out: Concurrent.Channel[String]) = Props(new ChunkActor(out))
  def propsAsync() = Props(new AsyncActor())
}

class AsyncActor () extends Actor with ActorLogging
{
  val clusterActor = Global.clusterSystem.actorOf(Props(classOf[ClusterActor], self))
  var origSender : ActorRef = _

  override def receive = {
    case fen:String =>
      log.info("FEN : "+fen)
      val analyseFen = AnalyseFen(fen)
      clusterActor ! analyseFen
      origSender = sender
      context.become(waitResult)
  }

  var lastResult : SocketProtocol.Analysis = _

  def waitResult (): Actor.Receive = {
    case res : SocketProtocol.Analysis =>
      lastResult = res

    case bestMove: SocketProtocol.BestMove =>

    case err: SocketProtocol.Error =>
      val response = JsObject(Seq("type"->JsString(err.messageType), "message"->Json.toJson(err)))
      origSender ! response
      self ! PoisonPill

    case SocketProtocol.Done =>
      val response = JsObject(Seq("type"->JsString(lastResult.messageType), "message"->Json.toJson(lastResult)))
      origSender ! (response.toString())
      self ! PoisonPill
  }
}

class ChunkActor (out: Concurrent.Channel[String]) extends Actor with ActorLogging
{
  val clusterActor = Global.clusterSystem.actorOf(Props(classOf[ClusterActor], self))

  override def receive = {
    case analyseFen:AnalyseFen =>
      clusterActor ! analyseFen
      context.become(waitResult)
  }

  def waitResult (): Actor.Receive = {
    case res: SocketProtocol.Analysis =>
      val response = JsObject(Seq("type" -> JsString(res.messageType), "message" -> Json.toJson(res)))
      out.push (response.toString())

    case bestMove: SocketProtocol.BestMove =>
      val response = JsObject(Seq("type" -> JsString(bestMove.messageType), "message" -> Json.toJson(bestMove)))
      out.push (response.toString())

    case err: SocketProtocol.Error =>
      val response = JsObject(Seq("type"->JsString(err.messageType), "message"->Json.toJson(err)))
      out.push (response.toString())
      out.end()
      self ! PoisonPill

    case SocketProtocol.Done =>
      val response = JsObject(Seq("type" -> JsString(SocketProtocol.RESPONSE_DONE)))
      out.push (response.toString())
      out.end()
      self ! PoisonPill
  }

}

class WebsocketActor (out: ActorRef) extends Actor with ActorLogging{

  val clusterActor = Global.clusterSystem.actorOf(Props(classOf[ClusterActor], self))

  override def receive = {
    case msg:String =>
      val request = SocketProtocol.parseRequest(msg)

      request match {
        case analyse: AnalyseFen =>
          clusterActor ! analyse
        case x =>
      }

      context.become(waitResult)
  }

  def waitResult (): Actor.Receive = {
    case res : SocketProtocol.Analysis =>
      val response = JsObject(Seq("type"->JsString(res.messageType), "message"->Json.toJson(res)))
      out ! (response.toString())

    case bestMove: SocketProtocol.BestMove =>
      val response = JsObject(Seq("type" -> JsString(bestMove.messageType), "message" -> Json.toJson(bestMove)))
      out ! (response.toString())

    case SocketProtocol.Done =>
      val response = JsObject(Seq("type"->JsString(SocketProtocol.RESPONSE_DONE)))
      out ! (response.toString())
      context.unbecome()
  }
}

class ClusterActor (out:ActorRef) extends Actor with ActorLogging{

  def nextWorkId(): String = UUID.randomUUID().toString

  override def receive = {
    case af : AnalyseFen =>
      log.info("Produced work: {}", af.fen)
      val work = Work(nextWorkId(), AnalyseJob(producer = self))
      Global.frontend ! work
      context.become(waitForWorker(af))
      context.setReceiveTimeout(10.seconds)

    case x =>
  }

  def waitForWorker (analyseFen: AnalyseFen): Actor.Receive = {
    case Frontend.Ok =>
      log.info("Work accepted.")

    case Frontend.NotOk =>
      log.warning("Work not accepted.")
      reportError(SocketProtocol.ERROR_WORKER_NOT_AVAILABLE)
      context.become (receive)

    case WorkerAssigned (worker : ActorRef) =>
      log.warning ("Worker assigned")
      context.become(analysisStarted(worker))
      context.setReceiveTimeout(Duration.Undefined)

      var millis = 5000 / 20 * analyseFen.skill.toInt
      millis = if (millis < 500) 500 else millis
      var duration = Duration (millis, MILLISECONDS)

      worker ! AnalyserProtocol.Start (analyseFen.fen, analyseFen.skill, duration)

    case m: Message =>
      log.warning ("User msg during waitAccepted. Returning to idle.")
      context.become(receive)
      self ! m

    case ReceiveTimeout =>
      log.info ("No response from worker. Unbecome.")
      context.setReceiveTimeout(Duration.Undefined)
      reportError(SocketProtocol.ERROR_WORKER_NOT_AVAILABLE)
      context.become(receive)

  }

  def analysisStarted (worker: ActorRef) : Receive = {

    case r: AnalyserProtocol.ResultUci =>
      log.info ("ResultUci : "+r)

      val evaluations = r.info.moveEvaluations.map {
        moveEvaluation =>
          val evalType = moveEvaluation.score.scoreType match {
            case uci.ScoreType.CentiPawns => SocketProtocol.EVAL_TYPE_CP
            case uci.ScoreType.Mate => SocketProtocol.EVAL_TYPE_MATE
          }
          Evaluation (evalType, moveEvaluation.score.value, moveEvaluation.moves)
      }

      val analysis = SocketProtocol.Analysis (r.info.depth, r.info.nodes, r.info.time, evaluations)
      out ! analysis

    case bm: AnalyserProtocol.BestMove =>
      val bestMove = SocketProtocol.BestMove (bm.move)
      out ! bestMove

    case f: AnalyserProtocol.Finished =>
      log.info ("Finished : "+f)
      out ! SocketProtocol.Done
      context.become (receive)

    case m: Message =>
      worker ! Stop
      context.become (receive)
      self ! m
  }

  def reportError (errorCode : Int)
  {
    val error = SocketProtocol.Error(errorCode)
    out ! error
  }

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.setReceiveTimeout(Duration.Undefined)

    case m : Message =>
      reportError(SocketProtocol.ERROR_INVALID_STATE)

    case x =>
      log.warning ("Ignoring message: "+x)
      super.unhandled(message)
  }

}
