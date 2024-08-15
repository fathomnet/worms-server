/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import io.circe.generic.auto.*
import org.fathomnet.worms.etc.circe.CirceCodecs.given
import org.fathomnet.worms.{ErrorMsg, SimpleWormsNode, StateController, WormsNode}
import sttp.tapir.Schema.annotations.format
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{query, PublicEndpoint, *}

import scala.concurrent.{ExecutionContext, Future}

class TaxaEndpoints(using ec: ExecutionContext) extends Endpoints:

    private val tag = "Taxa"

    // Make Tapir recursiive types happy
    // https://tapir.softwaremill.com/en/latest/endpoint/schemas.html#derivation-for-recursive-types-in-scala3
    implicit def wormsNodeSchema: Schema[WormsNode] = Schema.derived[WormsNode]

    // -- /taxa/descendants/:name
    /**
     * List all descendants of a taxon.
     */
    val taxaDescendants: PublicEndpoint[String, ErrorMsg, WormsNode, Any] = baseEndpoint
        .get
        .in("taxa" / "descendants")
        .in(path[String]("name"))
        .out(jsonBody[WormsNode])
        .description(
            "Return a tree structure of descendants from the provided name on down through the tree."
        )
        .tag(tag)

    val taxaDescendantsServerEndpoint: ServerEndpoint[Any, Future] =
        taxaDescendants.serverLogic((name: String) => Future(StateController.descendantTaxa(name)))

    val taxaAncestors: PublicEndpoint[String, ErrorMsg, WormsNode, Any] = baseEndpoint
        .get
        .in("taxa" / "ancestors")
        .in(path[String]("name"))
        .out(jsonBody[WormsNode])
        .description(
            "return a tree structure from the root of the taxonomic tree down to the given name. Note that the last node will have it's children trimmed off."
        )
        .tag(tag)

    val taxaAncestorsServerEndpoint: ServerEndpoint[Any, Future] =
        taxaAncestors.serverLogic((name: String) => Future(StateController.ancestorTaxa(name)))

    val taxaInfo: PublicEndpoint[String, ErrorMsg, SimpleWormsNode, Any] = baseEndpoint
        .get
        .in("taxa" / "info")
        .in(path[String]("name"))
        .out(jsonBody[SimpleWormsNode])
        .description("Returns the name, alternateNames, aphiaId, and rank of a term.")
        .tag(tag)

    val taxaInfoServerEndpoint: ServerEndpoint[Any, Future] =
        taxaInfo.serverLogic((name: String) => Future(StateController.taxaInfo(name)))

    val taxaParent: PublicEndpoint[String, ErrorMsg, SimpleWormsNode, Any] = baseEndpoint
        .get
        .in("taxa" / "parent")
        .in(path[String]("name"))
        .out(jsonBody[SimpleWormsNode])
        .description("Returns the name, alternateNames, aphiaId, and rank of the parent of the term.")
        .tag(tag)

    val taxaParentServerEndpoint: ServerEndpoint[Any, Future] =
        taxaParent.serverLogic((name: String) => Future(StateController.parentTaxa(name)))

    val taxaChildren: PublicEndpoint[String, ErrorMsg, List[SimpleWormsNode], Any] = baseEndpoint
        .get
        .in("taxa" / "children")
        .in(path[String]("name"))
        .out(jsonBody[List[SimpleWormsNode]])
        .description("Returns the name, alternateNames, aphiaId, and rank of the children of the term.")
        .tag(tag)

    val taxaChildrenServerEndpoint: ServerEndpoint[Any, Future] =
        taxaChildren.serverLogic((name: String) => Future(StateController.childTaxa(name)))

    val taxaQueryStartswith
        : PublicEndpoint[(String, Option[String], Option[String]), ErrorMsg, List[SimpleWormsNode], Any] =
        baseEndpoint
            .get
            .in("taxa" / "query" / "startswith")
            .in(path[String]("prefix"))
            .in(query[Option[String]]("rank"))
            .in(query[Option[String]]("parent"))
            .out(jsonBody[List[SimpleWormsNode]])
            .description("Returns a list of taxa that start with the given prefix.")
            .tag(tag)

    val taxaQueryStartswithEndpoint: ServerEndpoint[Any, Future] =
        taxaQueryStartswith.serverLogic((prefix: String, rank: Option[String], parent: Option[String]) =>
            Future(StateController.taxaByNameStartingWith(prefix, rank, parent))
        )

    val all: List[ServerEndpoint[Any, Future]] = List(
        taxaDescendantsServerEndpoint,
        taxaAncestorsServerEndpoint,
        taxaInfoServerEndpoint,
        taxaParentServerEndpoint,
        taxaChildrenServerEndpoint,
        taxaQueryStartswithEndpoint
    )
