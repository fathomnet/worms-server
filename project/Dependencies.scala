import sbt._

object Dependencies {

  private val circeVersion = "0.14.8"
  lazy val circeCore       = "io.circe" %% "circe-core"    % circeVersion
  lazy val circeGeneric    = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser     = "io.circe" %% "circe-parser"  % circeVersion

  lazy val jansi    = "org.fusesource.jansi"         % "jansi"           % "2.4.1"

  lazy val logback  = "ch.qos.logback"               % "logback-classic" % "1.5.6"
  lazy val methanol = "com.github.mizosoft.methanol" % "methanol"        % "1.7.0"
  lazy val munit    = "org.scalameta"               %% "munit"           % "1.0.0"
  lazy val picocli  = "info.picocli"                 % "picocli"         % "4.7.6"

  lazy val slf4jJdk     = "org.slf4j"      % "slf4j-jdk-platform-logging"   % "2.0.13"

  private val tapirVersion  = "1.10.9"
  lazy val tapirStubServer  = "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion
  lazy val tapirSwagger     = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
  lazy val tapirCirce       = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
  lazy val tapirCirceClient = "com.softwaremill.sttp.client3" %% "circe" % "3.9.7"
  lazy val tapirNetty       = "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion
  lazy val tapirVertx       = "com.softwaremill.sttp.tapir" %% "tapir-vertx-server" % tapirVersion

  lazy val typesafeConfig = "com.typesafe"   % "config"          % "1.4.3"
  lazy val zio            = "dev.zio"       %% "zio"             % "2.1.3"

}
