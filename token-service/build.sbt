name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"
lazy val akkaHttpVersion = "10.0.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)
