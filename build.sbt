import sbt._
import Keys._

sbtPlugin := true

organization := "chainkite"

name := "flyway-sbt-ext"


libraryDependencies := Seq(
  "com.typesafe" % "config" % "1.3.0"
)

resolvers += "Chainkite Repository" at "http://dl.bintray.com/chainkite/maven"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayReleaseOnPublish in ThisBuild := false
bintrayVcsUrl := 

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0")