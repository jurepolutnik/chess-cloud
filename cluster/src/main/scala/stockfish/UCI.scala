package uci


/**
 * Created with IntelliJ IDEA.
 * User: xlab
 * Date: 30/12/13
 * Time: 20:58
 * To change this template use File | Settings | File Templates.
 */


trait UCICommand { def command: String }
case class PositionCmd (posType: String, pos: String) extends UCICommand { def command = s"position $posType $pos" }
case class GoCmd (depth: Int, time: Int) extends UCICommand { def command = s"go depth $depth movetime $time" }
case class SetOptionCmd (name: String, value: String) extends UCICommand {def command=s"setoption name $name value $value"}
case object StopCmd extends UCICommand { def command = "stop" }



object ScoreType extends Enumeration{
  type ScoreType = Value
  val CentiPawns, Mate = Value
}
case class Score (scoreType: ScoreType.ScoreType, var value: Int)
case class MoveEvaluation (score: Score, moves: String)

case class MultiInfo (depth: Int, time: Int, nodes: Int, moveEvaluations: List[MoveEvaluation])
trait UCIResponse
case class BestMove (move: String, ponder: String) extends UCIResponse
case class Info (depth: Int, time: Int, nodes: Int, multipv: Int, moveEval: MoveEvaluation) extends UCIResponse

object Score
{
  val ScoreRegex = """(\w+) (-?\d+)""".r
  def apply (s: String) : Score =
  {
    val ScoreRegex (scoreType, scoreValue) = s

    scoreType match
    {
      case "mate" if scoreValue.toInt!=0 => Score(ScoreType.Mate, scoreValue.toInt)
      case _ => Score(ScoreType.CentiPawns, scoreValue.toInt)
    }
  }

}

object Info
{
  val InfoRegex = """info depth (\d+) seldepth \d+ score (\w+ -?\d+).* nodes (\d+) nps \d+ time (\d+) multipv (\d+) pv (.*)""".r

  def apply (s: String) : Option[Info] =
  {
    s match
    {
      case InfoRegex (depth, scoreTxt, nodes, time, multipv, pv) =>
        val score = Score (scoreTxt)
        val moveEval = MoveEvaluation(score, pv)
        Some (Info (depth.toInt, time.toInt, nodes.toInt, multipv.toInt, moveEval))

      case _ => None
    }
  }
}

object BestMove
{
  val BestMoveRegex = """bestmove (.*) ponder (.*)""".r

  def apply (s: String) : BestMove =
  {
    val BestMoveRegex (move, ponder) = s
    BestMove (move, ponder)
  }

}

