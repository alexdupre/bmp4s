import scala.scalanative.build._

name := "bmp4s"

ThisBuild / organization := "com.alexdupre"
ThisBuild / version := "0.5.1"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / scalacOptions := List("-feature", "-unchecked", "-deprecation", "-explaintypes", "-encoding", "UTF8", "-language:postfixOps")

lazy val bmp4s = (projectMatrix in file("."))
  .settings(
    name := "bmp4s"
  )
  .jvmPlatform(
    scalaVersions = Seq("2.13.5", "2.12.13", "2.11.12")
  )
  .nativePlatform(
    scalaVersions = Seq("2.13.5"),
  )

publish / skip := true

ThisBuild / publishTo := sonatypePublishToBundle.value

ThisBuild / licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

ThisBuild / homepage := Some(url("https://github.com/alexdupre"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/alexdupre/bmp4s"),
    "scm:git@github.com:alexdupre/bmp4s.git"
  )
)

ThisBuild / developers := List(
  Developer(id="alexdupre", name="Alex Dupre", email="ale@FreeBSD.org", url=url("https://github.com/alexdupre"))
)
