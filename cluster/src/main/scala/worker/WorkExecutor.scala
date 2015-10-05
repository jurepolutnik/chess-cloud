package worker

import akka.actor._
import com.typesafe.config.ConfigFactory
import util.CommandExecutor
import worker.AnalyserProtocol._
import scala.concurrent.duration._

class WorkExecutor extends Actor with ActorLogging{

  import context.dispatcher
  import uci._
  val stockfishPath = ConfigFactory.load().
                        withFallback(ConfigFactory.load("worker")).
                        getString("worker.stockfish.path")

  case object SendInfo
  var infoList : List[Info] = List()
  var bestMove : BestMove = null
  var sendTaskCancellable : Cancellable = _
  var scoreFix = 1;

  val exe = new CommandExecutor(stockfishPath,
    out => {
      log.info("INFO OUT: "+out)
      out match
      {
        case s if s.startsWith("info") =>
          val info = Info (s)
          if (info.isDefined)
          {
            if (info.get.moveEval.score.scoreType == ScoreType.CentiPawns)
              info.get.moveEval.score.value = info.get.moveEval.score.value * scoreFix
            log.info ("Info added to infoList.")
            infoList = info.get :: infoList
          }

        case s if s.startsWith("bestmove") =>
          bestMove = BestMove (s)
          self ! Finished()


        case s:String => log.info (s)
      }
    }
    ,
    err => {log.error("e: " + err); },
    code=> {
      log.info("Exiting with:"+code)
      code match
      {
        case 139 => throw new IllegalStateException() //Segmetation fault
      }
    }
  )

  def scheduler = context.system.scheduler

  def receive = {
  	case AnalyseJob (producer: ActorRef) =>
      log.info("Got job... assigning worker")
      producer ! WorkerAssigned(worker = self)
      context.become(working(sender, producer))
      context.setReceiveTimeout(3.second)
      log.info("Becoming working...")

    case any : Any =>
      log.warning("Unexpected message in receive state: "+any)
  }

  def working (worker: ActorRef, producer: ActorRef) : Receive = {

    case ReceiveTimeout =>
      log.info ("No response from producer. Unbecome.")
      context.setReceiveTimeout(Duration.Undefined)
      worker ! Worker.WorkComplete("II")
      context.unbecome()

  	case Start (fen: String, skill: String, time: Duration) =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info("Starting analysis...: "+fen)
      exe.write("ucinewgame")
      exe.write(SetOptionCmd("MultiPV", "3").command)
      exe.write(SetOptionCmd("Skill Level", skill).command)
      exe.write(PositionCmd("fen", fen).command)
      exe.write(GoCmd(50, time.toMillis.toInt).command)
      sendTaskCancellable = scheduler.scheduleOnce(1.seconds, self, SendInfo)
      if (time.isFinite())
      {
        val t = time.asInstanceOf[FiniteDuration]
        //scheduler.scheduleOnce(t, self, Stop)
      }
      if (fen.split(" "){1} == "w") // White to move
        scoreFix = 1;
      else // Black to move
        scoreFix = -1;


    case r : Result if sender != producer =>
        producer ! r

    case finished : Finished if sender != producer =>
      log.info ("Finishing!")
      if (!infoList.isEmpty) producer ! getInfo()
      if (bestMove != null) producer ! getBestMove()
      producer ! finished
      worker ! Worker.WorkComplete("II")
      context.unbecome()
      cleanup()

  	case Stop =>
      log.info ("Stopping!")
      exe.write("stop")
      scheduler.scheduleOnce(100.milliseconds, self, Finished())

    case SendInfo =>
      log.info ("Sending info.")
      if (!infoList.isEmpty) producer ! getInfo()
      sendTaskCancellable = scheduler.scheduleOnce(1.seconds, self, SendInfo)
  }

  def cleanup () =
  {
    infoList = List()
    bestMove = null
    if (sendTaskCancellable != null)
      sendTaskCancellable.cancel()
  }

  def getInfo () =
  {
    val first = infoList.head
    val moveEvaluations = infoList.take(3).map (info => info.moveEval).reverse
    val multiInfo = MultiInfo(first.depth, first.time, first.nodes, moveEvaluations)
    ResultUci (multiInfo)
  }

  def getBestMove () =
  {
    AnalyserProtocol.BestMove(bestMove.move)
  }

}

