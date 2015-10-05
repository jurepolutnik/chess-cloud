
name := "chesscloud-frontend"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DebianPlugin)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
	"com.typesafe.akka" %% "akka-contrib" % "2.3.4",
	"com.newrelic.agent.java" % "newrelic-agent" % "3.12.1",
	"com.newrelic.agent.java" % "newrelic-api" % "3.12.1"
)


includeFilter in (Assets, LessKeys.less) := "*.less"

mappings in Universal ++= {
  (baseDirectory.value / "data" ***).get pair relativeTo(baseDirectory.value)
}

//pipelineStages := Seq(rjs)
//pipelineStages := Seq(rjs,uglify)
//includeFilter in uglify := GlobFilter(".*/javascripts/*.js")


maintainer := "Jure Polutnik <jure.polutnik@gmail.com>"

packageDescription := "ChessCloud Frontend"

defaultLinuxInstallLocation := "/opt"


