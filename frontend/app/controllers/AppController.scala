package controllers

import actors.SocketProtocol.AnalyseFen
import play.api.libs.Comet
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.Play.current
import play.mvc.Results.{Chunks, StringChunks}

import scala.concurrent.duration._
import actors._
import akka.actor.Props
import misc.Global

import com.newrelic.api.agent.Trace

/**
 * User: Luigi Antonini
 * Date: 17/06/13
 * Time: 23:25
 */
object AppController extends Controller {

  import akka.pattern.ask
  implicit val timeout = akka.util.Timeout(1 second)

  def actorWebSocket = WebSocket.acceptWithActor[String, String] {
    request => out => ClusterActor.propsWebSocket (out)
  }


  @Trace
  def analyse(fen:String) =  Action.async {
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      val fenFixed = fen.replaceAll("%20", " ")

      val myActor = Akka.system.actorOf(Props[AsyncActor])
      (myActor ? fenFixed)(20.seconds).map(s => Ok(s.toString))
  }

  def comet () = Action {
    implicit request =>

      var fen, skill : String = ""
      request.queryString.map{
        case ("fen",v) => fen = v.mkString
        case ("skill",v) => skill = v.mkString
      }

      val analyseFen = new AnalyseFen(fen, skill)

      import scala.concurrent.ExecutionContext.Implicits.global

      val enumerator = Concurrent.unicast[String](onStart = channel => {
        val actor = Akka.system.actorOf(ClusterActor.propsChuncked(channel))
        actor ! analyseFen
      })

      Ok.chunked(enumerator &> Comet(callback="parent.update"))
  }
}
