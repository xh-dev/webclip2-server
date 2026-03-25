import java.io.PrintWriter
import java.nio.file.{Files, Paths}
import scala.beans.BeanProperty

name := "webclip2"

version := "0.0.8"

scalaVersion := "3.3.7"

val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.9"
val jacksonVersion = "3.1.0"
val jacksonAnnotationVersion = "2.21"
val pekkoVersion = "1.1.5"
val pekkoSecondVersion = "1.1.0"
val logbackVersion = "1.5.32"


libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-http" % pekkoSecondVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % pekkoSecondVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
  "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
  "tools.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "tools.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonAnnotationVersion,
  "tools.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "tools.jackson.core" % "jackson-databind" % jacksonVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "dev.xethh.utils" % "BinarySizeUtils" % "1.0.0",
  "dev.xethh.utils" % "BinarySizeUtilsJacksonExtension" % "1.0.0",
  "org.apache.pekko" %% "pekko-http-cors" % pekkoSecondVersion,

)
Compile / resourceDirectory := baseDirectory.value / "resources"

val mainName = Some("app.Main")
run / mainClass := mainName
assembly / assemblyJarName := "webclip2.jar"
assembly / mainClass  := mainName


assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", _*) => MergeStrategy.filterDistinctLines
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
