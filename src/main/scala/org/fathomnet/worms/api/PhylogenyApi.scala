/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import org.fathomnet.worms.{ErrorMsg, State}
import org.fathomnet.worms.{Data, Page}
import org.fathomnet.worms.etc.circe.CirceCodecs.{given, *}
import org.fathomnet.worms.etc.jdk.Logging.{given}
import org.scalatra.{BadRequest, ContentEncodingSupport, InternalServerError, NotFound, ScalatraServlet}
import scala.util.control.NonFatal



/**
 * REST API for the Worms Server.
 *
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
class PhylogenyApi extends ScalatraServlet with ContentEncodingSupport:

  private val log = System.getLogger(getClass.getName)

  before() {
    contentType = "application/json"
  }

  get("/names") {
    def limit = params.get("limit").map(_.toInt).getOrElse(100)
    def offset = params.get("offset").map(_.toInt).getOrElse(0)
    def search(data: Data): String =  //data.names.stringify
      val names = data.names.slice(offset, offset + limit).toSeq
      Page(names, limit, offset, data.names.size).stringify
    runSearch(search)
  }

  get("/names/count") {
    def search(data: Data): String = data.names.size.toString
    runSearch(search)
  }

  get("/query/startswith/:prefix") {
    val prefix = params("prefix").toLowerCase

    def search(data: Data): String =
      data.names.filter(_.toLowerCase.startsWith(prefix)).stringify

    runSearch(search)
  }

  get("/query/contains/:glob") {
    val glob = params("glob").toLowerCase

    def search(data: Data): String =
      data.names.filter(_.toLowerCase.contains(glob)).stringify

    runSearch(search)
  }

  get("/descendants/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find `$name`").stringify))
        case Some(node) => node.descendantNames.sorted.stringify

    runSearch(search)

  }

  get("/ancestors/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.buildParentPath(name).map(_.name).stringify

    runSearch(search)

  }

  get("/children/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find `$name`").stringify))
        case Some(node) => node.children.map(_.name).sorted.stringify

    runSearch(search)

  }

  get("/parent/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByChildName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find `$name`").stringify))
        case Some(node) => node.name.stringify

    runSearch(search)
  }

  get("/synonyms/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find `$name`").stringify))
        case Some(node) => node.names.stringify

    runSearch(search)
  }

  get("/taxa/descendants/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find: `$name`").stringify))
        case Some(node) => node.stringify

    runSearch(search)
  }

  get("/taxa/ancestors/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.buildParentTree(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find: `$name`").stringify))
        case Some(node) => node.stringify

    runSearch(search)
  }

  get("/taxa/info/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find `$name`").stringify))
        case Some(node) => node.simple.stringify

    runSearch(search)
  }

  get("/taxa/parent/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByChildName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find `$name`").stringify))
        case Some(node) => node.simple.stringify

    runSearch(search)
  }

  get("/taxa/children/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String =
      data.findNodeByName(name) match
        case None       => halt(NotFound(ErrorMsg(s"Unable to find `$name`").stringify))
        case Some(node) => node.children.map(_.simple).sortBy(_.name).stringify

    runSearch(search)

  }

  private def runSearch[A](search: Data => String) =
    State.data match
      case None       =>
        halt(
          NotFound(
            ErrorMsg(
              "The WoRMS dataset is missing. If this continues, please report it to the FathomNet team."
            ).stringify
          )
        )
      case Some(data) => 
        try 
          search(data)
        catch
          case NonFatal(e) =>
            log.atWarn.withCause(e).log("Error while running a search")
            halt(
              InternalServerError(
                ErrorMsg(
                  s"An unexpected error occurred: ${e.getMessage}. Did you ask for the entire WoRMS dataset in one request? Maybe don't do that."
                ).stringify
              )
            )
