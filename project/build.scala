import sbt._
import Keys._
import org.scalatra.sbt._
import org.scalatra.sbt.PluginKeys._

object GeonosisBuild extends Build {
  val ScalaVersion = "2.10.4"
  val ScalatraVersion = "2.2.2"

  lazy val project = Project (
    "geonosis",
    file("."),
    settings = Defaults.defaultSettings ++ ScalatraPlugin.scalatraWithJRebel ++ Seq(
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-json" % ScalatraVersion,
        "org.json4s"   %% "json4s-jackson" % "3.2.6",

        "org.clapper" %% "grizzled-slf4j" % "0.6.10",

        "org.apache.curator" % "curator-recipes" % "2.4.1",

        "com.typesafe" % "config" % "1.2.0",

        "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
        "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",

        "org.eclipse.jetty" % "jetty-webapp" % "8.1.9.v20130131" % "compile;container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "compile;container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar")),

        "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test"
      )
    )
  )
}
