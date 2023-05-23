/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import io.circe.generic.auto._
import org.fathomnet.worms.{Data, NotFound, Page, ServerError, State}
import org.fathomnet.worms.ErrorMsg
import org.fathomnet.worms.etc.circe.CirceCodecs.given
import org.fathomnet.worms.etc.jdk.Logging.given
import org.fathomnet.worms.SimpleWormsNode
import org.fathomnet.worms.StateController
import org.fathomnet.worms.WormsNode
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.{endpoint, query, stringBody, PublicEndpoint}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.Schema.annotations.format
import sttp.tapir.server.ServerEndpoint

class TaxaEndpoints(using ec: ExecutionContext) extends Endpoints:

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

  val taxaAncestorsServerEndpoint: ServerEndpoint[Any, Future] =
    taxaAncestors.serverLogic((name: String) => Future(StateController.ancestorTaxa(name)))

  val taxaInfo: PublicEndpoint[String, ErrorMsg, SimpleWormsNode, Any] = baseEndpoint
    .get
    .in("taxa" / "info")
    .in(path[String]("name"))
    .out(jsonBody[SimpleWormsNode])
    .description("Returns the name, alternateNames, aphiaId, and rank of a term.")

  val taxaInfoServerEndpoint: ServerEndpoint[Any, Future] =
    taxaInfo.serverLogic((name: String) => Future(StateController.taxaInfo(name)))

  val taxaParent: PublicEndpoint[String, ErrorMsg, SimpleWormsNode, Any] = baseEndpoint
    .get
    .in("taxa" / "parent")
    .in(path[String]("name"))
    .out(jsonBody[SimpleWormsNode])
    .description("Returns the name, alternateNames, aphiaId, and rank of the parent of the term.")

  val taxaParentServerEndpoint: ServerEndpoint[Any, Future] =
    taxaParent.serverLogic((name: String) => Future(StateController.parentTaxa(name)))

  val taxaChildren: PublicEndpoint[String, ErrorMsg, List[SimpleWormsNode], Any] = baseEndpoint
    .get
    .in("taxa" / "children")
    .in(path[String]("name"))
    .out(jsonBody[List[SimpleWormsNode]])
    .description("Returns the name, alternateNames, aphiaId, and rank of the children of the term.")

  val taxaChildrenServerEndpoint: ServerEndpoint[Any, Future] =
    taxaChildren.serverLogic((name: String) => Future(StateController.childTaxa(name)))

  val taxaQueryStartswith: PublicEndpoint[String, ErrorMsg, List[SimpleWormsNode], Any] = baseEndpoint
    .get
    .in("taxa" / "query" / "startswith")
    .in(path[String]("prefix"))
    .out(jsonBody[List[SimpleWormsNode]])
    .description("Returns a list of taxa that start with the given prefix.")

  val taxaQueryStartswithEndpoint: ServerEndpoint[Any, Future] =
    taxaQueryStartswith.serverLogic((prefix: String) => Future(StateController.taxaByNameStartingWith(prefix)))

  val all: List[ServerEndpoint[Any, Future]] = List(
    taxaDescendantsServerEndpoint,
    taxaAncestorsServerEndpoint,
    taxaInfoServerEndpoint,
    taxaParentServerEndpoint,
    taxaChildrenServerEndpoint,
    taxaQueryStartswithEndpoint
  )
