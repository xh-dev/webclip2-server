name := "webclip2"

version := "0.1"

scalaVersion := "2.13.5"

val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.9"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.13.3",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.13.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.3",
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "dev.xethh.utils" % "BinarySizeUtils" % "1.0.0",
  "dev.xethh.utils" % "BinarySizeUtilsJacksonExtension" % "1.0.0",
  "ch.megard" %% "akka-http-cors" % "1.1.3",
)

Compile / resourceDirectory := baseDirectory.value / "resources"

mainClass in (Compile, run) := Some("app.Main")
assemblyJarName in assembly := "webclip2.jar"
mainClass in assembly := Some("app.Main")

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}