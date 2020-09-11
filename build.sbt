organization := "com.alexdupre"

name := "bmp4s"

version := "0.4"

scalaVersion := "2.13.3"

scalacOptions := List("-feature", "-unchecked", "-deprecation", "-explaintypes", "-encoding", "UTF8", "-language:postfixOps")

publishTo := sonatypePublishToBundle.value

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

sonatypeProjectHosting := Some(xerial.sbt.Sonatype.GitHubHosting("alexdupre", "bmp4s", "Alex Dupre", "ale@FreeBSD.org"))
