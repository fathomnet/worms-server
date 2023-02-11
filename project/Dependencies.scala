import sbt._

object Dependencies {

  private val circeVersion = "0.14.4"
  lazy val circeCore       = "io.circe" %% "circe-core"    % circeVersion
  lazy val circeGeneric    = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser     = "io.circe" %% "circe-parser"  % circeVersion

  lazy val jansi    = "org.fusesource.jansi"         % "jansi"           % "2.4.0"

  private val jettyVersion = "9.4.45.v20220203"
  lazy val jettyServer     = "org.eclipse.jetty" % "jetty-server"   % jettyVersion
  lazy val jettyServlets   = "org.eclipse.jetty" % "jetty-servlets" % jettyVersion
  lazy val jettyWebapp     = "org.eclipse.jetty" % "jetty-webapp"   % jettyVersion

  private val jsoniterVersion = "2.13.7"
  lazy val jsoniterCore =  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % jsoniterVersion
  lazy val jsoniterMacros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterVersion

  lazy val logback  = "ch.qos.logback"               % "logback-classic" % "1.4.5"
  lazy val methanol = "com.github.mizosoft.methanol" % "methanol"        % "1.7.0"
  lazy val munit    = "org.scalameta"               %% "munit"           % "1.0.0-M7"
  lazy val picocli  = "info.picocli"                 % "picocli"         % "4.7.1"

  private val scalatraVersion = "2.8.2"
  lazy val scalatra           = ("org.scalatra" %% "scalatra"      % scalatraVersion).cross(CrossVersion.for3Use2_13)
  lazy val scalatraJson       = ("org.scalatra" %% "scalatra-json" % scalatraVersion).cross(CrossVersion.for3Use2_13)

  lazy val slf4jJdk     = "org.slf4j"      % "slf4j-jdk-platform-logging"   % "2.0.6"

  private val tapirVersion  = "1.2.8"
  lazy val tapirStubServer  = "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion
  lazy val tapirSwagger     = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
  lazy val tapirCirce       = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
  lazy val tapirCirceClient = "com.softwaremill.sttp.client3" %% "circe" % "3.6.2"
  lazy val tapirNetty       = "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion
  lazy val tapirVertx       = "com.softwaremill.sttp.tapir" %% "tapir-vertx-server" % tapirVersion
      // "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      // "ch.qos.logback" % "logback-classic" % "1.2.11",
  
      // "org.scalatest" %% "scalatest" % "3.2.12" % Test,
      

  lazy val typesafeConfig = "com.typesafe"   % "config"          % "1.4.2"
  lazy val zio            = "dev.zio"       %% "zio"             % "2.0.6"

}
