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
import org.fathomnet.worms.{Data, Names, NotFound, Page, ServerError, State}
import sttp.tapir.server.ServerEndpoint
import scala.concurrent.Future
import org.fathomnet.worms.ErrorMsg
import scala.util.control.NonFatal
import org.fathomnet.worms.etc.jdk.Logging.given
import scala.concurrent.ExecutionContext
import sttp.model.StatusCode
import org.fathomnet.worms.StateController

class NameEndpoints(using ec: ExecutionContext) extends Endpoints:

  // -- /names
  val namesEndpoint: PublicEndpoint[(Option[Int], Option[Int]), ErrorMsg, Page[String], Any] =
    baseEndpoint
      .get
      .in("names")
      .in(query[Option[Int]]("limit"))
      .in(query[Option[Int]]("offset"))
      .out(jsonBody[Page[String]])
      .description(
        "Returns all names ... there's a lot of names. The results are paged using query params limit (default 100) and offset (default 0)."
      )

  val namesServerEndpoint: ServerEndpoint[Any, Future] =
    namesEndpoint.serverLogic((limitOpt, offsetOpt) =>
      Future {
        val limit  = limitOpt.getOrElse(100)
        val offset = offsetOpt.getOrElse(0)
        StateController.findAllNames(limit, offset)
      }
    )

  // -- /names/count
  val namesCountEndpoint: PublicEndpoint[Unit, ErrorMsg, Int, Any] = baseEndpoint
    .get
    .in("names" / "count")
    .out(jsonBody[Int])
    .description("Returns the total number of names available.")

  val namesCountServerEndpoint: ServerEndpoint[Any, Future] =
    namesCountEndpoint.serverLogic(Unit => Future.successful(StateController.countAllNames()))

  val namesByAphiaId: PublicEndpoint[Long, ErrorMsg, Names, Any] = baseEndpoint
    .get
    .in("names" / "aphiaid")
    .in(path[Long]("aphiaid"))
    .out(jsonBody[Names])
    .description("Returns the data for a name given its aphiaid.")

  val namesByAphiaIdServerEndpoint: ServerEndpoint[Any, Future] =
    namesByAphiaId.serverLogic(aphiaid => Future(StateController.findNamesByAphiaId(aphiaid)))

  // --/query/startswith/:prefix
  val queryStartswithEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("query" / "startswith")
    .in(path[String]("prefix"))
    .out(jsonBody[List[String]])
    .description("Returns all names that start with the given prefix.")

  val queryStartswithServerEndpoint: ServerEndpoint[Any, Future] =
    queryStartswithEndpoint.serverLogic(prefix =>
      Future(StateController.queryNamesStartingWith(prefix))
    )

  // --/query/contains/:glob
  val queryContainsEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("query" / "contains")
    .in(path[String]("glob"))
    .out(jsonBody[List[String]])
    .description("Returns all names that contain the given string (glob).")

  val queryContainsServerEndpoint: ServerEndpoint[Any, Future] =
    queryContainsEndpoint.serverLogic(glob => Future(StateController.queryNamesContaining(glob)))

  // -- /descendants/:name
  val descendantsEndpoint: Endpoint[Unit, (String, Option[Boolean]), ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("descendants")
    .in(path[String]("name"))
    .in(query[Option[Boolean]]("accepted").description("Only include accepted names. Allowed values are true or false. Default is false."))
    .out(jsonBody[List[String]])
    .description(
      "Return the primary names of the taxa and all its descendants in alphabetical order."
    )

  val descendantsServerEndpoint: ServerEndpoint[Any, Future] =
    descendantsEndpoint.serverLogic((name, accepted) => Future(StateController.descendantNames(name, accepted.getOrElse(false))))

  // -- /ancestors/:name
  val ancestorsEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("ancestors")
    .in(path[String]("name"))
    .out(jsonBody[List[String]])
    .description(
      "Return the primary names of all the ancestors in order from the top of the taxa tree down."
    )

  val ancestorsServerEndpoint: ServerEndpoint[Any, Future] =
    ancestorsEndpoint.serverLogic(name => Future(StateController.ancestorNames(name)))

  // -- /children/:name
  val childrenEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("children")
    .in(path[String]("name"))
    .out(jsonBody[List[String]])
    .description("Return the primary names of the direct children.")

  val childrenServerEndpoint: ServerEndpoint[Any, Future] =
    childrenEndpoint.serverLogic(name => Future(StateController.childNames(name)))

  // -- /parent/:name
  val parentEndpoint: PublicEndpoint[String, ErrorMsg, String, Any] = baseEndpoint
    .get
    .in("parent")
    .in(path[String]("name"))
    .out(jsonBody[String])
    .description("Return the name of the parent of {name}.")

  val parentServerEndpoint: ServerEndpoint[Any, Future] =
    parentEndpoint.serverLogic(name => Future(StateController.parentName(name)))

  // -- /synonyms/:name
  val synonymsEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("synonyms")
    .in(path[String]("name"))
    .out(jsonBody[List[String]])
    .description(
      "Returns alternative names for a term. The first term in the list is the primary/accepted name."
    )

  val synonymsServerEndpoint: ServerEndpoint[Any, Future] =
    synonymsEndpoint.serverLogic(name => Future(StateController.synonyms(name)))

  override val all: List[ServerEndpoint[Any, Future]] = List(
    namesServerEndpoint,
    namesCountServerEndpoint,
    namesByAphiaIdServerEndpoint,
    queryStartswithServerEndpoint,
    queryContainsServerEndpoint,
    descendantsServerEndpoint,
    ancestorsServerEndpoint,
    childrenServerEndpoint,
    parentServerEndpoint,
    synonymsServerEndpoint
  )
