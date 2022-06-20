/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import _root_.org.fathomnet.worms.{ErrorMsg, NotFound, ServerError}

import _root_.org.fathomnet.worms.{Data, State}
import _root_.org.fathomnet.worms.etc.jdk.Logging.given
import scala.util.control.NonFatal
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.model.StatusCode
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import io.circe.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import scala.concurrent.Future

trait Endpoints:

  val log = System.getLogger(getClass.getName)

  def all: List[ServerEndpoint[Any, Future]]

  val baseEndpoint = endpoint.errorOut(
    oneOf[ErrorMsg](
      oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
      oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError]))
    )
  )
