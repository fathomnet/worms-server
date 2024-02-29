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
          nameEndpoints.namesCountEndpoint,
          nameEndpoints.namesEndpoint,
          nameEndpoints.namesByAphiaId,
          nameEndpoints.ancestorsEndpoint,
          nameEndpoints.childrenEndpoint,
          nameEndpoints.descendantsEndpoint,
          nameEndpoints.parentEndpoint,
          nameEndpoints.queryContainsEndpoint,
          nameEndpoints.queryStartswithEndpoint,
          nameEndpoints.synonymsEndpoint,
          taxaEndpoints.taxaAncestors,
          taxaEndpoints.taxaChildren,
          taxaEndpoints.taxaDescendants,
          taxaEndpoints.taxaInfo,
          taxaEndpoints.taxaParent,
          taxaEndpoints.taxaQueryStartswith
        ),
        AppConfig.Name,
        AppConfig.Version
      )
