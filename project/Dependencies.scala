import sbt._

object Dependencies {

  private val circeVersion = "0.14.1"
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

  lazy val logback  = "ch.qos.logback"               % "logback-classic" % "1.3.0-alpha14"
  lazy val methanol = "com.github.mizosoft.methanol" % "methanol"        % "1.6.0"
  lazy val munit    = "org.scalameta"               %% "munit"           % "1.0.0-M2"
  lazy val picocli  = "info.picocli"                 % "picocli"         % "4.6.1"

  private val scalatraVersion = "2.8.2"
  lazy val scalatra           = ("org.scalatra" %% "scalatra"      % scalatraVersion).cross(CrossVersion.for3Use2_13)
  lazy val scalatraJson       = ("org.scalatra" %% "scalatra-json" % scalatraVersion).cross(CrossVersion.for3Use2_13)

  lazy val slf4jJdk     = "org.slf4j"      % "slf4j-jdk-platform-logging"   % "2.0.0-alpha7"
  lazy val typesafeConfig = "com.typesafe"   % "config"          % "1.4.2"
  lazy val zio            = "dev.zio"       %% "zio"             % "1.0.12"

}
