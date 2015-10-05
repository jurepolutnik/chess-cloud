import com.typesafe.sbt.SbtNativePackager._

name := "chesscloud-cluster"

version := "0.1"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-contrib" % "2.3.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.4",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalatest" % "scalatest_2.10" % "2.1.6" % "test",
  "commons-io" % "commons-io" % "2.4" % "test")


packageArchetype.java_server

maintainer := "Jure Polutnik"

packageDescription := "ChessCloud Cluster"

defaultLinuxInstallLocation := "/opt"
