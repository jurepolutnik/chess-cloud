package actors

import play.api.libs.json.{JsString, JsObject, Json}
import play.api.Logger

/**
 * Created with IntelliJ IDEA.
 * User: xlab
 * Date: 01/02/14
 * Time: 19:02
 * To change this template use File | Settings | File Templates.
 */
object SocketProtocol {

  abstract class SocketMessage (@transient val messageType: String)

  val ERROR_INVALID_STATE = 1
  val ERROR_WORKER_NOT_AVAILABLE = 2

  val REQUEST_ANALYSE = "request_analyse"
  val RESPONSE_ANALYSIS = "response_analyse"
  val RESPONSE_BESTMOVE = "response_bestmove"
  val RESPONSE_DONE = "response_done"
  val RESPONSE_OK = "response_ok"
  val RESPONSE_ERROR = "response_error"

  val EVAL_TYPE_CP = "cp"
  val EVAL_TYPE_MATE = "mate"

  def parseRequest (request: String) : SocketMessage =
  {
    val json = Json.parse (request)
    Logger.info ("JSON: " + json.toString())

    val msgType = json \ "type"
    Logger.info ("TYPE: " + msgType.toString())
    val msgMessage = json \ "message"
    Logger.info ("MSG: " + msgMessage.toString())

    msgType.as[String] match
    {
      case REQUEST_ANALYSE => msgMessage.as[AnalyseFen]
      case x =>
        Logger.warn("Unknown type "+msgType)
        return null
    }
  }

  // Requests
  case class AnalyseFen (fen: String, skill: String = "20") extends SocketMessage(REQUEST_ANALYSE)

  // Responses
  case class Error (errorCode: Int) extends SocketMessage(RESPONSE_ERROR)
  case class Ok () extends SocketMessage (RESPONSE_OK)
  case class Done () extends SocketMessage (RESPONSE_DONE)

  case class BestMove (move: String) extends SocketMessage(RESPONSE_BESTMOVE)
  case class Analysis (depth: Int, nodes:Int, time:Int, evaluations: List[Evaluation]) extends SocketMessage(RESPONSE_ANALYSIS)
  case class Evaluation (scoreType: String, scoreValue: Int, moves: String )

  // // // // // // // // //
  // JSON Writes/Reads
  //
  //implicit val socketMessageWrites = Json.writes[SocketMessage]
  implicit val analyseFenWrites = Json.writes[AnalyseFen]
  implicit val errorWrites = Json.writes[Error]
  implicit val evaluationWrites = Json.writes[Evaluation]
  implicit val responseWrites = Json.writes[Analysis]
  implicit val bestmoveWrites = Json.writes[BestMove]

  implicit val analyseFenReads = Json.reads[AnalyseFen]
  implicit val errorReads = Json.reads[Error]
  implicit val evaluationReads = Json.reads[Evaluation]
  implicit val responseReads = Json.reads[Analysis]
  implicit val bestmoveReads = Json.reads[BestMove]
}
