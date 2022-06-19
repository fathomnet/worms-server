/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import sttp.tapir.server.ServerEndpoint
import scala.concurrent.Future
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import org.fathomnet.worms.AppConfig

case class SwaggerEndpoints(nameEndpoints: NameEndpoints, taxaEndpoints: TaxaEndpoints):

  val all: List[ServerEndpoint[Any, Future]] =
    SwaggerInterpreter()
      .fromEndpoints[Future](
        List(
          nameEndpoints.namesEndpoint,
          nameEndpoints.namesCountEndpoint,
          nameEndpoints.ancestorsEndpoint,
          nameEndpoints.descendantsEndpoint,
          nameEndpoints.parentEndpoint,
          nameEndpoints.childrenEndpoint,
          nameEndpoints.queryStartswithEndpoint,
          nameEndpoints.queryContainsEndpoint,
          taxaEndpoints.taxaAncestors,
          taxaEndpoints.taxaDescendants,
          taxaEndpoints.taxaParent,
          taxaEndpoints.taxaChildren,
          taxaEndpoints.taxaInfo
        ),
        AppConfig.Name,
        AppConfig.Version
      )
