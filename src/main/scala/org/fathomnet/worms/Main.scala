/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import _root_.io.vertx.core.Vertx
import _root_.io.vertx.ext.web.Router
import org.fathomnet.worms.api.{NameEndpoints, SwaggerEndpoints, TaxaEndpoints}
import org.fathomnet.worms.etc.jdk.CustomExecutors
import org.fathomnet.worms.etc.jdk.CustomExecutors.*
import org.fathomnet.worms.etc.jdk.Logging.given
import org.fathomnet.worms.io.WormsLoader
import org.fathomnet.worms.io.extended.CombineTrees.combine
import org.fathomnet.worms.io.extended.{CombineTrees, ExtendedLoader}
import picocli.CommandLine
import picocli.CommandLine.{Command, Option as Opt, Parameters}
import sttp.tapir.server.vertx.VertxFutureServerInterpreter
import sttp.tapir.server.vertx.VertxFutureServerInterpreter.*

import java.nio.file.Path
import java.util as ju
import java.util.concurrent.Callable
import scala.compiletime.uninitialized
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

@Command(
    description = Array("The Worms Server"),
    name = "main",
    mixinStandardHelpOptions = true,
    version = Array("0.0.1")
)
class MainRunner extends Callable[Int]:

    @Opt(
        names = Array("-p", "--port"),
        description = Array("The server port. Default is ${DEFAULT-VALUE}")
    )
    private var port: Int = Option(System.getenv("WORMS_PORT")).map(_.toInt).getOrElse(8080)

    // "/Users/brian/Downloads/worms"
    @Parameters(index = "0", description = Array("Path to the WoRMS data file directory"))
    var path: Path = uninitialized

    @Parameters(
        index = "1..*",
        description = Array(
            "Paths to extra tree files (CSV with 3 columns: id, parentId, names).  [Optional]",
            "These will becombined with the main WoRMS tree. This will create a new root (object)",
            "All the nodes from these files will have synthetic aphiaIds with negative values."
        )
    )
    var treeFiles: Array[Path] = Array.empty[Path]

    override def call(): Int =
        Main.run(port, path, treeFiles.toList)
        0

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
object Main:

    private val log = System.getLogger(getClass.getName)

    def main(args: Array[String]): Unit =
        new CommandLine(new MainRunner()).execute(args*)

    // def run(port: Int, wormsDir: Path): Unit =
    //   log.atInfo.log(s"Starting up ${AppConfig.Name} v${AppConfig.Version}")

    //   // Lood data off main thread
    //   given executionContext: ExecutionContext =
    //     CustomExecutors.newFixedThreadPoolExecutor(20).asScala
    //   executionContext.execute(() => State.data = WormsLoader.load(wormsDir).map(n => Data(n)))

    //   val nameEndpoints    = NameEndpoints()
    //   val taxaEndpoints    = TaxaEndpoints()
    //   val swaggerEndpoints = SwaggerEndpoints(nameEndpoints, taxaEndpoints)
    //   val allEndpoints     = nameEndpoints.all ++ taxaEndpoints.all ++ swaggerEndpoints.all

    //   val program = for
    //     binding <- NettyFutureServer()
    //                  .port(port)
    //                  .addEndpoints(allEndpoints)
    //                  .start()
    //     stop    <- binding.stop()
    //   yield stop

    //   Await.result(program, Duration.Inf)

    def run(port: Int, wormsDir: Path, treeFiles: List[Path] = Nil): Unit =
        log.atInfo.log(s"Starting up ${AppConfig.Name} v${AppConfig.Version} on port $port")

        // Lood data off main thread
        given executionContext: ExecutionContext =
            CustomExecutors.newFixedThreadPoolExecutor(20).asScala
        executionContext.execute(() => State.data = load(wormsDir, treeFiles).map(n => Data(n)))

        val nameEndpoints    = NameEndpoints()
        val taxaEndpoints    = TaxaEndpoints()
        val swaggerEndpoints = SwaggerEndpoints(nameEndpoints, taxaEndpoints)
        val allEndpoints     = nameEndpoints.all ++ taxaEndpoints.all ++ swaggerEndpoints.all

        val vertx  = Vertx.vertx()
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)

        for endpoint <- allEndpoints do
            val attach = VertxFutureServerInterpreter().route(endpoint)
            attach(router)

        Await.result(server.requestHandler(router).listen(port).asScala, Duration.Inf)

    def load(wormsDir: Path, treeFiles: List[Path])(using ec: ExecutionContext): Option[WormsNode] =
        WormsLoader.load(wormsDir).map { root =>
            if (treeFiles.nonEmpty)
                // Our new base. We use 0 as aphiaId so that the real aphiaIds are not incremented when the trees are combined
                val newRoot      = WormsNode("object", "", 0L, 0L, Nil, Nil)
                val newBranches  = treeFiles.flatMap(ExtendedLoader.load(_)).toSeq
                val trees        = root +: newBranches
                val combinedRoot = CombineTrees.combine(newRoot, trees, root.maxAphiaId)
                // We need to reset the aphiaId to -1 so that it's obivious that the root is not a real aphiaId
                combinedRoot.copy(aphiaId = -1L)
            else root
        }
