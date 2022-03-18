/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.ext.methanol

import com.github.mizosoft.methanol.Methanol
import com.github.mizosoft.methanol.Methanol.Interceptor.Chain

import java.net.http.{HttpHeaders, HttpRequest, HttpResponse}
import java.time.{Duration, Instant}
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

/**
 * Adds logging to Methanol, JDK HTTP Client
 * @author Brian Schlining
 * @since 2022-03-17
 */
object LoggingInterceptor extends Methanol.Interceptor:

  private val log = System.getLogger(getClass.getName)

  override def intercept[T](request: HttpRequest, chain: Chain[T]): HttpResponse[T] =
    logRequest(request)
    toLoggingChain(request, chain).forward(request)

  override def interceptAsync[T](
      request: HttpRequest,
      chain: Chain[T]
  ): CompletableFuture[HttpResponse[T]] =
    logRequest(request)
    toLoggingChain(request, chain).forwardAsync(request)

  private def logRequest(request: HttpRequest): Unit =
    log.log(
      System.Logger.Level.DEBUG,
      () => s""" Sent >>>
        |${request.method()} ${request.uri()}
        |${headersToString(request.headers())}""".stripMargin.trim()
    )

  private def toLoggingChain[T](request: HttpRequest, chain: Chain[T]): Chain[T] =

    val sentAt = Instant.now()
    // format: off
    chain.withBodyHandler(responseInfo =>
      log.log(System.Logger.Level.DEBUG, () =>
          s""" Received <<< ${request.method()} ${request.uri()} in ${Duration.between(sentAt, Instant.now()).toMillis()}ms
          |${responseInfo.statusCode()}
          |${headersToString(responseInfo.headers())}""".stripMargin.trim()
        )
      chain.bodyHandler().apply(responseInfo)
    )
    // format: on

  private def headersToString(headers: HttpHeaders): String =
    headers
      .map()
      .entrySet()
      .stream()
      .map(e => s"${e.getKey()}: ${String.join(", ", e.getValue())}")
      .collect(Collectors.joining(System.lineSeparator()))
      .trim()
