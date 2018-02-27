name := "persistence"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  val akkaVersion = "2.5.10"
  Seq(
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1"
  )
}