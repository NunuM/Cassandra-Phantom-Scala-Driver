name := "cassandra_phantom_scala_driver"

version := "1.0"

scalaVersion := "2.11.8"


libraryDependencies ++= Seq(
  "com.outworkers" % "phantom-dsl_2.11" % "2.5.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25"
)