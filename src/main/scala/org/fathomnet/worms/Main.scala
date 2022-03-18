/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms

import java.{util => ju}
import java.util.ArrayList
import java.util.concurrent.Callable
import org.eclipse.jetty.server._
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import picocli.CommandLine
import picocli.CommandLine.{Command, Option => Opt, Parameters}
import org.fathomnet.worms.io.WormsLoader
import java.nio.file.Path
import java.nio.file.Paths

@Command(
  description = Array("A Main app"),
  name = "main",
  mixinStandardHelpOptions = true,
  version = Array("0.0.1")
)
class MainRunner extends Callable[Int]:

  @Opt(names = Array("-p", "--port"), description = Array("A message"))
  private var port: Int = 8080

  // "/Users/brian/Downloads/worms"
  @Parameters(index = "0", description = Array("Path to the WoRMS data file directory"))
  var path: Path = _

  override def call(): Int =
    Main.run(port, path)
    0

object Main:

  def main(args: Array[String]): Unit =
    new CommandLine(new MainRunner()).execute(args: _*)

  def run(port: Int, wormsDir: Path): Unit =

    State.data = WormsLoader.load(wormsDir).map(n => Data(n))

    val server: Server = new Server
    server.setStopAtShutdown(true)

    val httpConfig = new HttpConfiguration()
    httpConfig.setSendDateHeader(true)
    httpConfig.setSendServerVersion(false)

    val connector = new NetworkTrafficServerConnector(
      server,
      new HttpConnectionFactory(httpConfig)
    )
    connector.setPort(port)
    println(s"Starting Scalatra on port $port")
    connector.setIdleTimeout(90000)
    server.addConnector(connector)

    val webApp = new WebAppContext
    webApp.setContextPath("/")
    webApp.setResourceBase("webapp")
    webApp.setEventListeners(Array(new ScalatraListener))

    server.setHandler(webApp)

    server.start()
