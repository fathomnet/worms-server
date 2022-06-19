/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import sttp.tapir.{endpoint, query, stringBody, PublicEndpoint}
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import org.fathomnet.worms.{Data, NotFound, Page, ServerError, State}
import sttp.tapir.server.ServerEndpoint
import scala.concurrent.Future
import org.fathomnet.worms.ErrorMsg
import scala.util.control.NonFatal
import org.fathomnet.worms.etc.jdk.Logging.given
import scala.concurrent.ExecutionContext
import sttp.model.StatusCode
import org.fathomnet.worms.WormsNode
import sttp.tapir.Schema.annotations.format
import org.fathomnet.worms.SimpleWormsNode
import org.fathomnet.worms.etc.circe.CirceCodecs.given

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
    .description("Return a tree structure of descendants from the provided name on down through the tree.")

  val taxaDescendantsServerEndpoint: ServerEndpoint[Any, Future] =
    taxaDescendants.serverLogic((name: String) =>
      Future {
        def search(data: Data): Option[WormsNode] =
          data.findNodeByName(name)

        runNodeSearch(search, s"Unable to find `$name`")
      }
    )

  val taxaAncestors: PublicEndpoint[String, ErrorMsg, WormsNode, Any] = baseEndpoint
    .get
    .in("taxa" / "ancestors")
    .in(path[String]("name"))
    .out(jsonBody[WormsNode])
    .description("return a tree structure from the root of the taxonomic tree down to the given name. Note that the last node will have it's children trimmed off.")

  val taxaAncestorsServerEndpoint: ServerEndpoint[Any, Future] =
    taxaAncestors.serverLogic((name: String) =>
      Future {
        def search(data: Data): Option[WormsNode] =
          data.buildParentTree(name)
        runNodeSearch(search, s"Unable to find `$name`")
      }
    )

  val taxaInfo: PublicEndpoint[String, ErrorMsg, SimpleWormsNode, Any] = baseEndpoint
    .get
    .in("taxa" / "info")
    .in(path[String]("name"))
    .out(jsonBody[SimpleWormsNode])
    .description("Returns the name, alternateNames, aphiaId, and rank of a term.")

  val taxaInfoServerEndpoint: ServerEndpoint[Any, Future] =
    taxaInfo.serverLogic((name: String) =>
      Future {
        def search(data: Data): Option[SimpleWormsNode] =
          data.findNodeByName(name).map(_.simple)
        runNodeSearch(search, s"Unable to find `$name`")
      }
    )

  val taxaParent: PublicEndpoint[String, ErrorMsg, SimpleWormsNode, Any] = baseEndpoint
    .get
    .in("taxa" / "parent")
    .in(path[String]("name"))
    .out(jsonBody[SimpleWormsNode])
    .description("Returns the name, alternateNames, aphiaId, and rank of the parent of the term.")

  val taxaParentServerEndpoint: ServerEndpoint[Any, Future] =
    taxaParent.serverLogic((name: String) =>
      Future {
        def search(data: Data): Option[SimpleWormsNode] =
          data.findNodeByChildName(name).map(_.simple)
        runNodeSearch(search, s"Unable to find `$name`")
      }
    )

  val taxaChildren: PublicEndpoint[String, ErrorMsg, List[SimpleWormsNode], Any] = baseEndpoint
    .get
    .in("taxa" / "children")
    .in(path[String]("name"))
    .out(jsonBody[List[SimpleWormsNode]])
    .description("Returns the name, alternateNames, aphiaId, and rank of the children of the term.")

  val taxaChildrenServerEndpoint: ServerEndpoint[Any, Future] =
    taxaChildren.serverLogic((name: String) =>
      Future {
        def search(data: Data): Option[WormsNode] =
          data.findNodeByName(name)

        runNodeSearch(search, s"Unable to find `$name`")
          .map(_.children.map(_.simple).sortBy(_.name).toList)

      }
    )

  val all: List[ServerEndpoint[Any, Future]] = List(
    taxaDescendantsServerEndpoint,
    taxaAncestorsServerEndpoint,
    taxaInfoServerEndpoint,
    taxaParentServerEndpoint,
    taxaChildrenServerEndpoint
  )

  private def runNodeSearch[A](search: Data => Option[A], errorMsg: String): Either[ErrorMsg, A] =
    runSearch(search).fold(
      e => Left(e),
      node =>
        node match
          case Some(n) => Right(n)
          case None    => Left(NotFound(errorMsg))
    )
