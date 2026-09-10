/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import _root_.io.circe.{Decoder, Encoder, Json}
import _root_.io.vertx.core.Vertx
import _root_.io.vertx.ext.web.Router
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.vertx.{VertxFutureServerInterpreter, VertxFutureServerOptions}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.util.concurrent.{Callable, ConcurrentHashMap, Executor, Executors, ThreadFactory}
import scala.concurrent.{ExecutionContext, Future}

final case class Marker(value: String)

/**
 * Pins down the tapir/Vert.x threading behaviour that `Main` relies on to keep the taxa endpoints off the event loop.
 *
 * The non-obvious part: mounting a route with `blockingRoute` is *not* enough. Tapir's default server options build a
 * VertxExecutionContext bound to the request's event loop, so the response body is encoded back on the event loop even
 * for a blocking route. Encoding a large WormsNode tree is the expensive part, so the interpreter also needs a
 * `specificExecutionContext`.
 *
 * @author
 *   Brian Schlining
 * @since 2026-09-10
 */
class VertxBlockingContextSuite extends munit.FunSuite:

    private val observed = ConcurrentHashMap[String, String]()

    private def named(prefix: String): ThreadFactory = r =>
        val t = Thread(r)
        t.setName(prefix)
        t.setDaemon(true)
        t

    private val logicEc = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2, named("LOGIC-POOL")))

    /** Records the thread that encodes the response body. */
    private given Encoder[Marker] = (m: Marker) =>
        observed.put(m.value, Thread.currentThread.getName)
        Json.obj("value" -> Json.fromString(m.value))

    private given Decoder[Marker] = Decoder.forProduct1("value")(Marker.apply)

    private def serverEndpoint(label: String): ServerEndpoint[Any, Future] =
        endpoint
            .get
            .in(label)
            .out(jsonBody[Marker])
            .serverLogicSuccess[Future](_ => Future(Marker(label))(using logicEc))

    /** The same worker-pool ExecutionContext that Main installs on its taxa interpreter. */
    private def vertxBlockingExecutionContext(vertx: Vertx): ExecutionContext =
        val executor: Executor = runnable =>
            val task: Callable[Void] = () =>
                runnable.run()
                null
            vertx.executeBlocking(task, false)
        ExecutionContext.fromExecutor(executor)

    test("the response body is encoded off the event loop only when the interpreter has a blocking ExecutionContext"):
        val vertx  = Vertx.vertx()
        val router = Router.router(vertx)

        try
            val default  = VertxFutureServerInterpreter()
            val blocking = VertxFutureServerInterpreter(
                VertxFutureServerOptions
                    .default
                    .copy(
                        specificExecutionContext = Some(vertxBlockingExecutionContext(vertx))
                    )
            )

            default.route(serverEndpoint("route-default"))(router)
            default.blockingRoute(serverEndpoint("blockingroute-default"))(router)
            blocking.blockingRoute(serverEndpoint("blockingroute-workerec"))(router)

            val server =
                vertx.createHttpServer().requestHandler(router).listen(0).toCompletionStage.toCompletableFuture.get()
            val client = HttpClient.newHttpClient()

            for label <- List("route-default", "blockingroute-default", "blockingroute-workerec") do
                val request  =
                    HttpRequest.newBuilder(URI.create(s"http://localhost:${server.actualPort}/$label")).build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                assertEquals(response.statusCode(), 200, s"$label returned ${response.body()}")

            def encodedOn(label: String): String =
                Option(observed.get(label)).getOrElse(fail(s"No encoding thread recorded for $label"))

            // How it used to be mounted: the body is encoded on the event loop.
            assert(
                encodedOn("route-default").contains("eventloop"),
                s"Expected an event loop thread, got ${encodedOn("route-default")}"
            )

            // blockingRoute on its own does NOT move encoding off the event loop.
            assert(
                encodedOn("blockingroute-default").contains("eventloop"),
                s"Expected an event loop thread, got ${encodedOn("blockingroute-default")}"
            )

            // How Main mounts the taxa endpoints.
            assert(
                !encodedOn("blockingroute-workerec").contains("eventloop"),
                s"Expected a worker thread, got ${encodedOn("blockingroute-workerec")}"
            )
        finally vertx.close().toCompletionStage.toCompletableFuture.get()
