
name := "arbet"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

version := "1.0-SNAPSHOT"

val akkaV = "2.3.6"

val sprayV = "1.3.1"

libraryDependencies ++= Seq(
  ws,
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "betfair" % "betfair-service-ng" % "0.1-SNAPSHOT" from "file:///home/e/IdeaProjects/arbet/lib/betfair-service-ng_2.11-0.1-SNAPSHOT.jar",
  "io.spray" %% "spray-can" % sprayV,
  "io.spray" %% "spray-caching" % sprayV,
//  "com.typesafe.akka" %% "akka-slf4j" % akkaV,
  "io.spray" %% "spray-client" % sprayV,
  "io.spray" %% "spray-routing" % sprayV,
//  "io.spray" % "spray-testkit" % sprayV,
  "net.sourceforge.htmlunit" % "htmlunit" % "2.27",
  "io.spray" %% "spray-json" % sprayV
)

enablePlugins(JavaServerAppPackaging)
mappings in Universal += file("appspec.yml") -> "appspec.yml"
mappings in Universal += file("buildspec.yml") -> "buildspec.yml"
mappings in Universal ++= mapDirectoryAndContents((file("scripts"),"scripts"))
//mappings in Universal += file("scripts") -> "scripts"


//playScalaSettings
