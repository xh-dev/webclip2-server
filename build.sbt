import java.io.PrintWriter
import java.nio.file.{Files, Paths}
import scala.beans.BeanProperty

name := "webclip2"

version := "0.0.3"

scalaVersion := "2.13.5"

val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.9"
libraryDependencies ++= Seq(
  ("com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-stream" % AkkaVersion).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-http" % AkkaHttpVersion).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion).cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test).cross(CrossVersion.for3Use2_13),
  ("com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" %% "akka-slf4j" % AkkaVersion).cross(CrossVersion.for3Use2_13),
  "com.fasterxml.jackson.core" % "jackson-core" % "2.13.3",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.13.3",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.13.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.3",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "dev.xethh.utils" % "BinarySizeUtils" % "1.0.0",
  "dev.xethh.utils" % "BinarySizeUtilsJacksonExtension" % "1.0.0",
  ("ch.megard" %% "akka-http-cors" % "1.1.3").cross(CrossVersion.for3Use2_13),
)

Compile / resourceDirectory := baseDirectory.value / "resources"

val mainName = Some("app.Main")
run / mainClass := mainName
assembly / assemblyJarName := "webclip2.jar"
assembly / mainClass  := mainName


assembly / assemblyMergeStrategy := {
  case "reference.conf" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val buildPrepare = taskKey[Unit]("Executes steps which are needed before sbt build")

lazy val branchName = settingKey[String]("The name of the Github branch which will prefix the RPM")
lazy val commitId = settingKey[String]("The name of the Github branch which will prefix the RPM")

branchName := sys.props.getOrElse("branchName", sys.env.getOrElse("branchName", "unknown"))
commitId := sys.props.getOrElse("commitId", sys.env.getOrElse("commitId", "unknown"))

buildPrepare := {
  val versionTxt = "version.txt"
  val dir = Paths.get("resources")
  val versionFile = new File(dir.toFile,versionTxt)
  println(versionFile)
  if(!versionFile.exists()){
    versionFile.createNewFile()
  }
  println(versionFile.getAbsoluteFile)

  case class Version(
                      @BeanProperty branch: String,
                      @BeanProperty version: String,
                      @BeanProperty commit: String,
                    )
  val v = Version(branchName.value, version.value, commitId.value)

  Files.deleteIfExists(versionFile.toPath)
  Files.createFile(versionFile.toPath)
  Some(new PrintWriter(versionFile)).foreach{p => p.write("branch: \"%s\"\nversion: \"%s\"\ncommit: \"%s\"\n".format(branchName.value, version.value, commitId.value)); p.close()}
}

Compile / compile := ((Compile / compile) dependsOn buildPrepare).value
