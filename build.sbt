import sbt._
import Keys._

sbtPlugin := true

organization := "chainkite"

name := "flyway-sbt-ext"

libraryDependencies := Seq(
  "com.typesafe" % "config" % "1.3.0"
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayReleaseOnPublish in ThisBuild := false
bintrayRepository := "sbt-plugins"

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0")