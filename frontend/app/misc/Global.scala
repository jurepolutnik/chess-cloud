package misc

import java.io.File

import com.fasterxml.jackson.databind.JsonNode
import controllers.Assets
import play.api._
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc._
import play.api.mvc.Results._
import controllers.routes
import play.libs.Json
import scala.concurrent.Future
import akka.actor._
import com.typesafe.config.{Config, ConfigFactory}
import worker.Frontend
import workers.WorkResultConsumer
import scala.collection.JavaConversions._

import play.api.Play.current

import scala.io.Source
import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: luigi
 * Date: 18/04/13
 * Time: 00:19
 * To change this template use File | Settings | File Templates.
 */
object Global extends GlobalSettings {

  var clusterSystem : ActorSystem = _
  var frontend : ActorRef = _

  override def onStart(app: Application) {

    try{
      initCluster(app)
    }
    catch{
      case e: Exception =>
        Logger.warn ("Unable to initialize Akka cluster : "+e.getMessage)
    }
  }

  def initCluster (app: Application) = {

    var conf:Config = null

    if (play.api.Play.isDev(app))
    {
      Logger.info("In DEV mode : don't overwrite seed nodes")
      conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
          withFallback(ConfigFactory.load("cluster"))
    }
    else
    {
      Logger.info("In PROD mode : overwrite seed nodes")
      val clusterFile = play.api.Play.getFile ("conf/cluster.conf")
      conf =  ConfigFactory.parseFile(clusterFile)

      val seeds = conf.getStringList("cluster.seeds")

      val akkaSeeds = seeds.map {
          addr ⇒ "akka.tcp://ClusterSystem@" + addr
      }.toList

      val propAkkaSeeds = "[\""+akkaSeeds.mkString("\",\"")+"\"]"
      Logger.info("akka.cluster.seed-nodes="+propAkkaSeeds)

      conf = ConfigFactory.parseString("akka.cluster.seed-nodes="+propAkkaSeeds).
        withFallback(conf)
    }

    clusterSystem = ActorSystem("ClusterSystem", conf)

    frontend = clusterSystem.actorOf(Props[Frontend], "frontend")
    clusterSystem.actorOf(Props[WorkResultConsumer], "consumer")

  }

  override def onHandlerNotFound(request: RequestHeader) = {
    import play.api.libs.concurrent.Execution.Implicits._
    Future(Redirect(routes.Application.index()))
  }

  override def onStop(app: Application) = {
  }

}
