/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import org.fathomnet.worms.AppConfig
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.Future

case class SwaggerEndpoints(
    nameEndpoints: NameEndpoints,
    taxaEndpoints: TaxaEndpoints,
    detailEndpoints: DetailEndpoints
):

    val all: List[ServerEndpoint[Any, Future]] =
        SwaggerInterpreter()
            .fromEndpoints[Future](
                List(
                    detailEndpoints.detailsEndpoint,
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
