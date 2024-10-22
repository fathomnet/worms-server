/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import scala.concurrent.ExecutionContext
import org.fathomnet.worms.WormsDetails

import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{query, PublicEndpoint, *}
import org.fathomnet.worms.etc.circe.CirceCodecs.given
import scala.concurrent.Future
import org.fathomnet.worms.StateController

class DetailEndpoints(using ec: ExecutionContext) extends Endpoints {


    private val tag = "Details"

    val detailsEndpoint = 
        baseEndpoint
            .get
            .in("details")
            .in(path[String]("name"))
            .out(jsonBody[WormsDetails])
            .description("Returns details about a worms taxa.")
            .tag(tag)

    val detailsServerEndpoint: ServerEndpoint[Any, Future] =
        detailsEndpoint.serverLogic((name: String) => Future(StateController.details(name)))


    override def all: List[ServerEndpoint[Any, Future]] = List(detailsServerEndpoint)
  
}
