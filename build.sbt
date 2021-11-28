scalaVersion := "2.13.3"

name := "scala_jdbc"
organization := "org.modak"
version := "1.0"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.scalikejdbc" %% "scalikejdbc"               % "3.3.5",
  "ch.qos.logback"  %  "logback-classic"           % "1.2.3",
  "mysql" % "mysql-connector-java" % "8.0.26",
  "com.lihaoyi" %% "os-lib" % "0.7.8"
)
val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
