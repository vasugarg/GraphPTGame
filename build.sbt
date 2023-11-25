import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "GraphPTGame"
  )

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaHttpV      = "10.2.10"
  val akkaV          = "2.6.20"
  val circeV         = "0.14.4"
  val scalaTestV     = "3.2.15"
  val akkaHttpCirceV = "1.39.2"
  val guavaVersion = "31.0.1-jre"
  val logbackVersion = "1.2.10"

  Seq(
    "io.circe"          %% "circe-core" % circeV,
    "io.circe"          %% "circe-parser" % circeV,
    "io.circe"          %% "circe-generic" % circeV,
    "org.scalatest"     %% "scalatest" % scalaTestV % "test"
  ) ++ Seq(
    "com.google.guava" % "guava" % guavaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
    "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % "test",
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.20" % Test,
    "io.spray" %% "spray-json" % "1.3.6",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.10",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    //"org.slf4j" % "slf4j-api" % "1.7.30"
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    //"org.slf4j" % "slf4j-api" % "2.0.5"
  )
}

Compile / mainClass := Some("cs441.HW3.server.GameApp")
Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary


val jarName = "graphPTGame.jar"
assembly/assemblyJarName := jarName
