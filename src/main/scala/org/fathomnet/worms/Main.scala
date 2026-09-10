/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import _root_.io.vertx.core.Vertx
import _root_.io.vertx.ext.web.Router
import org.fathomnet.worms.api.{DetailEndpoints, NameEndpoints, SwaggerEndpoints, TaxaEndpoints}
import org.fathomnet.worms.etc.jdk.CustomExecutors
import org.fathomnet.worms.etc.jdk.CustomExecutors.*
import org.fathomnet.worms.etc.jdk.Logging.given
import org.fathomnet.worms.io.WormsLoader

import scala.util.control.NonFatal
// import org.fathomnet.worms.io.extended.CombineTrees.combine
import org.fathomnet.worms.io.extended.{CombineTrees, ExtendedLoader}
import picocli.CommandLine
import picocli.CommandLine.{Command, Option as Opt, Parameters}
import sttp.tapir.server.vertx.VertxFutureServerInterpreter.*
import sttp.tapir.server.vertx.{VertxFutureServerInterpreter, VertxFutureServerOptions}

import java.nio.file.Path
import java.util.concurrent.{Callable, Executor}
import scala.compiletime.uninitialized
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import org.fathomnet.worms.io.WormsConcept

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

    def run(port: Int, wormsDir: Path, treeFiles: List[Path] = Nil): Unit =
        log.atInfo.log(s"Starting up ${AppConfig.Name} v${AppConfig.Version} on port $port")

        // Lood data off main thread
        given executionContext: ExecutionContext =
            CustomExecutors.newFixedThreadPoolExecutor(20).asScala
        executionContext.execute(() =>
            val (wormsConcepts, root) = load(wormsDir, treeFiles)
            val data                  = root.map(r => Data(r, wormsConcepts))
            State.data = data
        )

        val vertx  = Vertx.vertx()
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)

        // The taxa endpoints walk and serialize whole subtrees, which for something like
        // `/taxa/descendants/Animalia` is far too much work for an event loop. They get their own
        // interpreter that keeps every stage in a Vert.x blocking context:
        //
        //   - `blockingRoute` dispatches the handler to a worker thread instead of the event loop
        //   - `specificExecutionContext` keeps tapir's own stages there too
        //
        // The second half matters as much as the first. Left at its default, tapir builds a
        // VertxExecutionContext bound to the request's event loop, so the response body is encoded
        // back on the event loop no matter how the route was mounted -- and encoding the tree is
        // where most of the time goes. See VertxBlockingContextSuite.
        val blockingEc          = vertxBlockingExecutionContext(vertx)
        val blockingInterpreter = VertxFutureServerInterpreter(
            VertxFutureServerOptions.default.copy(specificExecutionContext = Some(blockingEc))
        )
        val interpreter         = VertxFutureServerInterpreter()

        val nameEndpoints    = NameEndpoints()
        val taxaEndpoints    = TaxaEndpoints(using blockingEc)
        val detailEndpoints  = DetailEndpoints()
        val swaggerEndpoints = SwaggerEndpoints(nameEndpoints, taxaEndpoints, detailEndpoints)

        // Mounted in the original order: Vert.x matches routes in registration order.
        val routes =
            nameEndpoints.all.map(e => interpreter.route(e)) ++
                taxaEndpoints.all.map(e => blockingInterpreter.blockingRoute(e)) ++
                detailEndpoints.all.map(e => interpreter.route(e)) ++
                swaggerEndpoints.all.map(e => interpreter.route(e))

        for attach <- routes do attach(router)

        Await.result(server.requestHandler(router).listen(port).asScala, Duration.Inf)

    /**
     * An ExecutionContext that runs its tasks on the Vert.x worker pool, i.e. in a blocking context, rather than on an
     * event loop.
     *
     * @param vertx
     *   The Vertx instance whose worker pool the tasks are submitted to
     */
    private def vertxBlockingExecutionContext(vertx: Vertx): ExecutionContext =
        val executor: Executor = runnable =>
            val task: Callable[Void] = () =>
                try
                    runnable.run()
                    null
                catch
                    case NonFatal(t) =>
                        log.atError.withCause(t).log("A task failed on the Vert.x worker pool")
                        throw t
            // ordered = false, otherwise tasks sharing a context are run one at a time
            vertx.executeBlocking(task, false)
        ExecutionContext.fromExecutor(
            executor,
            t => log.atError.withCause(t).log("A taxa endpoint failed on the Vert.x worker pool")
        )

    def load(wormsDir: Path, treeFiles: List[Path])(using
        ec: ExecutionContext
    ): (Seq[WormsConcept], Option[WormsNode]) =
        val (wormsConcepts, rootOpt) = WormsLoader.load(wormsDir)
        val newRoot                  = rootOpt.map { root =>
            if (treeFiles.nonEmpty)
                try
                    // Our new base. We use 0 as aphiaId so that the real aphiaIds are not incremented when the trees are combined
                    val newRoot      = WormsNode("object", "", 0L, 0L, Nil, Nil)
                    val newBranches  = treeFiles.flatMap(ExtendedLoader.load).toSeq
                    val trees        = root +: newBranches
                    val combinedRoot = CombineTrees.combine(newRoot, trees, root.maxAphiaId)
                    // We need to reset the aphiaId to -1 so that it's obivious that the root is not a real aphiaId
                    combinedRoot.copy(aphiaId = -1L)
                catch
                    case NonFatal(e) =>
                        log.atWarn.withCause(e).log("Error combining trees from " + treeFiles.mkString(", "))
                        root
            else root
        }
        (wormsConcepts, newRoot)
