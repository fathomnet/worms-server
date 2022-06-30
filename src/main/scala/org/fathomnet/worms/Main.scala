/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import java.{util => ju}
import java.util.ArrayList
import java.util.concurrent.Callable
import picocli.CommandLine
import picocli.CommandLine.{Command, Option => Opt, Parameters}
import org.fathomnet.worms.io.WormsLoader
import java.nio.file.Path
import java.nio.file.Paths
import org.fathomnet.worms.etc.jdk.Logging.given
import scala.concurrent.ExecutionContext
import org.fathomnet.worms.api.{NameEndpoints, SwaggerEndpoints, TaxaEndpoints}
import org.fathomnet.worms.etc.jdk.CustomExecutors
import org.fathomnet.worms.etc.jdk.CustomExecutors.*
import sttp.tapir.server.netty.NettyFutureServer

import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Future

@Command(
  description = Array("The Worms Server"),
  name = "main",
  mixinStandardHelpOptions = true,
  version = Array("0.0.1")
)
class MainRunner extends Callable[Int]:

  @Opt(names = Array("-p", "--port"), description = Array("The server port"))
  private var port: Int = Option(System.getenv("WORMS_PORT")).map(_.toInt).getOrElse(8080)

  // "/Users/brian/Downloads/worms"
  @Parameters(index = "0", description = Array("Path to the WoRMS data file directory"))
  var path: Path = _

  override def call(): Int =
    Main.run(port, path)
    0

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
object Main:

  private val log = System.getLogger(getClass.getName)

  def main(args: Array[String]): Unit =
    new CommandLine(new MainRunner()).execute(args: _*)

  def run(port: Int, wormsDir: Path): Unit =
    log.atInfo.log(s"Starting up ${AppConfig.Name} v${AppConfig.Version}")

    // Lood data off main thread
    given executionContext: ExecutionContext =
      CustomExecutors.newFixedThreadPoolExecutor(20).asScala
    executionContext.execute(() => State.data = WormsLoader.load(wormsDir).map(n => Data(n)))

    val nameEndpoints    = NameEndpoints()
    val taxaEndpoints    = TaxaEndpoints()
    val swaggerEndpoints = SwaggerEndpoints(nameEndpoints, taxaEndpoints)
    val allEndpoints     = nameEndpoints.all ++ taxaEndpoints.all ++ swaggerEndpoints.all

    val program = for
      binding <- NettyFutureServer()
                   .port(port)
                   .addEndpoints(allEndpoints)
                   .start()
      _       <- Future {
                   println(s"Go to http://localhost:${port}/docs to open SwaggerUI. [ctrl]-C to exit.")
                   StdIn.readLine()
                 }
      stop    <- binding.stop()
    yield stop

    Await.result(program, Duration.Inf)
