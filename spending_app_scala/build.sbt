name := "spending_app"

version := "1.0"

lazy val `spending_app` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq(
    ehcache, ws, specs2 % Test, guice)
libraryDependencies ++= Seq(
    "org.postgresql" % "postgresql" % "42.2.12",
    "com.typesafe.play" %% "play-slick" % "5.0.0",
    "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
    "com.typesafe.play" %% "play-json" % "2.9.2"
)