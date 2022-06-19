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

class NameEndpoints(using ec: ExecutionContext) extends Endpoints:

  // -- /names
  val namesEndpoint: PublicEndpoint[(Option[Int], Option[Int]), ErrorMsg, Page[String], Any] =
    baseEndpoint
      .get
      .in("names")
      .in(query[Option[Int]]("limit"))
      .in(query[Option[Int]]("offset"))
      .out(jsonBody[Page[String]])
      .description("Returns all names ... there's a lot of names. The results are paged using query params limit (default 100) and offset (default 0).")

  val namesServerEndpoint: ServerEndpoint[Any, Future] =
    namesEndpoint.serverLogic((limitOpt, offsetOpt) =>
      Future {
        val limit                            = limitOpt.getOrElse(100)
        val offset                           = offsetOpt.getOrElse(0)
        def search(data: Data): Page[String] = //data.names.stringify
          val names = data.names.slice(offset, offset + limit).toSeq
          Page(names, limit, offset, data.names.size)

        runSearch(search)
      }
    )

  // -- /names/count
  val namesCountEndpoint: PublicEndpoint[Unit, ErrorMsg, Int, Any] = baseEndpoint
    .get
    .in("names" / "count")
    .out(jsonBody[Int])
    .description("Returns the total number of names available.")

  val namesCountServerEndpoint: ServerEndpoint[Any, Future] =
    namesCountEndpoint.serverLogic(Unit => Future.successful(runSearch(data => data.names.size)))

  // --/query/startswith/:prefix
  val queryStartswithEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("query" / "startswith")
    .in(path[String]("prefix"))
    .out(jsonBody[List[String]])
    .description("Returns all names that start with the given prefix.")

  val queryStartswithServerEndpoint: ServerEndpoint[Any, Future] =
    queryStartswithEndpoint.serverLogic(prefix =>
      Future {
        def search(data: Data): List[String] =
          data.names.filter(_.toLowerCase.startsWith(prefix)).toList
        runSearch(search)
      }
    )

  // --/query/contains/:glob
  val queryContainsEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("query" / "contains")
    .in(path[String]("glob"))
    .out(jsonBody[List[String]])
    .description("Returns all names that contain the given string (glob).")

  val queryContainsServerEndpoint: ServerEndpoint[Any, Future] =
    queryContainsEndpoint.serverLogic(glob =>
      Future {
        def search(data: Data): List[String] =
          data.names.filter(_.toLowerCase.contains(glob)).toList
        runSearch(search)
      }
    )

  // -- /descendants/:name
  val descendantsEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("descendants")
    .in(path[String]("name"))
    .out(jsonBody[List[String]])
    .description("Return the primary names of the taxa and all its descendants in alphabetical order.")

  val descendantsServerEndpoint: ServerEndpoint[Any, Future] =
    descendantsEndpoint.serverLogic(name =>
      Future {
        def search(data: Data): List[String] =
          data.findNodeByName(name) match
            case None       => Nil
            case Some(node) => node.descendantNames.sorted.toList
        runSearch(search).fold(
          e => Left(e),
          v =>
            v match
              case Nil => Left(NotFound(s"Unable to find `$name`"))
              case _   => Right(v)
        )
      }
    )

  // -- /ancestors/:name
  val ancestorsEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("ancestors")
    .in(path[String]("name"))
    .out(jsonBody[List[String]])
    .description("Return the primary names of all the ancestors in order from the top of the taxa tree down.")

  val ancestorsServerEndpoint: ServerEndpoint[Any, Future] =
    ancestorsEndpoint.serverLogic(name =>
      Future {
        def search(data: Data): List[String] =
          data.buildParentPath(name).map(_.name)
        runSearch(search)
      }
    )

  // -- /children/:name
  val childrenEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("children")
    .in(path[String]("name"))
    .out(jsonBody[List[String]])
    .description("Return the primary names of the direct children.")

  val childrenServerEndpoint: ServerEndpoint[Any, Future] =
    childrenEndpoint.serverLogic(name =>
      Future {
        def search(data: Data): List[String] =
          data.findNodeByName(name) match
            case None       => Nil
            case Some(node) => node.children.map(_.name).sorted.toList
        runSearch(search).fold(
          e => Left(e),
          v => if (v.isEmpty) Left(NotFound(s"Unable to find `$name`")) else Right(v)
        )
      }
    )

  // -- /parent/:name
  val parentEndpoint: PublicEndpoint[String, ErrorMsg, String, Any] = baseEndpoint
    .get
    .in("parent")
    .in(path[String]("name"))
    .out(jsonBody[String])
    .description("Return the name of the parent of {name}.")

  val parentServerEndpoint: ServerEndpoint[Any, Future] =
    parentEndpoint.serverLogic(name =>
      Future {
        def search(data: Data): Option[String] =
          data.findNodeByChildName(name).map(_.name)
        runSearch(search).fold(
          e => Left(e),
          v =>
            v match
              case None    => Left(NotFound(s"Unable to find `$name`"))
              case Some(p) => Right(p)
        )
      }
    )

  // -- /synonyms/:name
  val synonymsEndpoint: PublicEndpoint[String, ErrorMsg, List[String], Any] = baseEndpoint
    .get
    .in("synonyms")
    .in(path[String]("name"))
    .out(jsonBody[List[String]])
    .description("Returns alternative names for a term. The first term in the list is the primary/accepted name.")

  val synonymsServerEndpoint: ServerEndpoint[Any, Future] =
    synonymsEndpoint.serverLogic(name =>
      Future {
        def search(data: Data): List[String] =
          data.findNodeByName(name) match
            case None       => Nil
            case Some(node) => node.names.toList

        runSearch(search).fold(
          e => Left(e),
          v => if (v.isEmpty) Left(NotFound(s"Unable to find `$name`")) else Right(v)
        )
      }
    )

  override val all: List[ServerEndpoint[Any, Future]] = List(
    namesServerEndpoint,
    namesCountServerEndpoint,
    queryStartswithServerEndpoint,
    queryContainsServerEndpoint,
    descendantsServerEndpoint,
    ancestorsServerEndpoint,
    childrenServerEndpoint,
    parentServerEndpoint,
    synonymsServerEndpoint
  )
