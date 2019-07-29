import mill._, scalalib._, scalafmt._, publish._
import ammonite.ops._
import $ivy.`com.lihaoyi::mill-contrib-bloop:0.5.0`

trait ScalaModuleCommon extends ScalaModule with ScalafmtModule {
  def scalaVersion = "2.12.2"

  def scalacPluginIvyDeps = Agg(ivy"org.scala-lang.plugins:scala-continuations-plugin_2.12.2:1.0.3")
  def scalacOptions = Seq("-P:continuations:enable")
}

object `akka-frp` extends ScalaModuleCommon with PublishModule {
  def publishVersion = "0.0.1"
  def pomSettings = PomSettings(
    description = "Akka+FRP",
    organization = "uosis",
    url = "https://github.com/uosis/akka-frp",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("uosis", "akka-frp"),
    developers = Seq(
      Developer("uosis", "Uosis Levandauskas","https://github.com/uosis")
    )
  )

  def ivyDeps = Agg(
    ivy"ch.qos.logback:logback-classic:1.1.3",
    ivy"com.typesafe.scala-logging::scala-logging:3.5.0",
    ivy"com.typesafe.akka::akka-actor:2.4.17",
    ivy"com.typesafe.akka::akka-slf4j:2.4.17",
    ivy"io.dylemma::scala-frp-sync:1.3",
    ivy"org.scala-lang.plugins::scala-continuations-library:1.0.3",
  )

  object tests extends Tests {
    def ivyDeps        = Agg(
      ivy"com.lihaoyi::utest:0.7.1",
      ivy"com.typesafe.akka::akka-testkit:2.4.17",
    )
    def testFrameworks = Seq("utest.runner.Framework")
  }

  object examples extends ScalaModuleCommon {
    def moduleDeps = Seq(`akka-frp`)
  }
}
