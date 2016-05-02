import com.github.retronym.SbtOneJar._

organization := "abb.com"

name := "esxtools"

version := "0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.7"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers += Resolver.bintrayRepo("pathikrit", "maven")
 
libraryDependencies ++= Seq(
  "org.scalatest"              %% "scalatest"        % "2.2.4"    % "test",
  "com.github.pathikrit"       %% "better-files"     % "2.15.0",
  "com.typesafe.scala-logging" %% "scala-logging"    % "3.1.0",
  "com.github.scopt"           %% "scopt"            % "3.4.0",
  "com.lihaoyi"                %% "pprint"           % "0.3.8",
  "com.toastcoders"            %  "yavijava"         % "6.0.03",
  "commons-lang"               %  "commons-lang"     % "2.6",
  "ch.qos.logback"             %  "logback-classic"  % "1.1.3",
  "com.typesafe.akka"          %%  "akka-actor"  % "2.4.2"
)

// ----- Start of OneJar settings
oneJarSettings

// http://stackoverflow.com/questions/14595852/renaming-jar-files-with-sbt-when-using-sbtonejar
// This gets rid of the trailing "-one-jar"
artifact in oneJar <<= moduleName(Artifact(_))

// rename the jar
artifact in oneJar ~= { (art: Artifact) =>
  art.copy(`type` = "jar", extension = "jar", name = art.name + "-one-jar")
}
// ----- End of OneJar settings

// ----- Start of sbt-buildinfo settings
lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, organization),
    buildInfoPackage := "hello"
  )
// ----- End of sbt-buildinfo settings
