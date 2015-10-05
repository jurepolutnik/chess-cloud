package worker

import scala.concurrent.duration._
import com.typesafe.config._
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterSingletonManager
import akka.japi.Util.immutableSeq
import akka.actor.AddressFromURIString
import akka.actor.ActorPath
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.Identify
import akka.actor.ActorIdentity
import akka.persistence.journal.leveldb.SharedLeveldbJournal

object Main {

  val ROLE_MASTER = "master"
  val ROLE_WORKER = "worker"
  val ROLE_AUTO_LOCAL = "auto"

  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.load()

    val role = config.getString("cluster.role")

    if (role == ROLE_AUTO_LOCAL) {
      val port = if(args.length > 0) args(0).toInt else -1

      startLocalCluster(port)
    }
    else
    {
      startClusterNode (role)
    }

  }


  def getSeedsString (conf: Config): ConfigList  =
  {
    import scala.collection.JavaConversions._

    val seeds = conf.getStringList("cluster.seeds")
    val akkaSeeds = seeds map {
      ip ⇒ s"akka.tcp://ClusterSystem@$ip"
    }

    ConfigValueFactory.fromIterable(akkaSeeds)
  }

  val KEY_CONTACT_POINTS = "contact-points"
  val KEY_CLUSTER_SEED_NODES = "akka.cluster.seed-nodes"
  val KEY_REMOTE_TCP_PORT = "akka.remote.netty.tcp.port"

  def startClusterNode (role: String): Unit = {
    val conf = ConfigFactory.load()
    val seeds = getSeedsString(conf)

    if (role == ROLE_WORKER)
    {
      val workerConf = ConfigFactory.empty()
        .withValue(KEY_CONTACT_POINTS, seeds).
        withFallback(conf)

      startWorker(workerConf)
    }
    else if (role == ROLE_MASTER)
    {
      val masterConf = ConfigFactory.empty()
        .withValue(KEY_CLUSTER_SEED_NODES, seeds).
        withFallback(conf)

      startBackend(masterConf)
    }
  }

  def startLocalCluster (port: Int): Unit = {

    val seeds = getSeedsString(ConfigFactory.load())

    val masterConfOrig = ConfigFactory.empty().
        withValue(KEY_CLUSTER_SEED_NODES, seeds).
        withFallback(ConfigFactory.load("master"))

    val workerConfOrig  = ConfigFactory.empty().
        withValue(KEY_CONTACT_POINTS, seeds).
        withFallback(ConfigFactory.load("worker"))


    if (port<0) {
      val masterConf = ConfigFactory.parseString("%s=%s" format (KEY_REMOTE_TCP_PORT,"2551")).
        withFallback(masterConfOrig)

      val workerConf = ConfigFactory.parseString("%s=%s" format (KEY_REMOTE_TCP_PORT,"0")).
        withFallback(workerConfOrig)

      startBackend(masterConf)
      Thread.sleep(5000)
      startWorker(workerConf)
    } else {
      if (2000 <= port && port <= 2999) {
        val masterConf = ConfigFactory.parseString("%s=%s" format (KEY_REMOTE_TCP_PORT, port)).
          withFallback(masterConfOrig)
        startBackend(masterConf)
      }
      else if (3000 <= port && port <= 3999) {
        val frontendConf = ConfigFactory.parseString("%s=%s" format (KEY_REMOTE_TCP_PORT, port)).
          withFallback(masterConfOrig)
        startFrontend(frontendConf)
      }
      else
      {
        val workerConf = ConfigFactory.parseString("%s=%s" format (KEY_REMOTE_TCP_PORT, port)).
          withFallback(workerConfOrig)
        startWorker(workerConf)
      }
    }
  }

  def workTimeout = 10.seconds

  def startBackend(conf : Config): Unit = {

    val system = ActorSystem("ClusterSystem", conf)
    system.actorOf(ClusterSingletonManager.props(Master.props(workTimeout), "active",
      PoisonPill, Some("backend")), "master")
  }

  def startWorker(conf :Config): Unit = {

    // load worker.conf
    val system = ActorSystem("WorkerSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")

    val size = conf.getInt("worker.size")
    for (num <- 0 until size)
    {
      system.actorOf(Worker.props(clusterClient, Props[WorkExecutor]), "worker-"+(num+1))

    }
  }

  def startFrontend(conf: Config): Unit = {
    val system = ActorSystem("ClusterSystem", conf)
    val frontend = system.actorOf(Props[Frontend], "frontend")
    system.actorOf(Props(classOf[WorkProducer], frontend), "producer")
    system.actorOf(Props[WorkResultConsumer], "consumer")
  }

}
