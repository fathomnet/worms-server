/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import org.scalatra.ScalatraServlet
import org.fathomnet.worms.{ErrorMsg, State}
import org.fathomnet.worms.etc.circe.CirceCodecs.{given, *}
import org.scalatra.BadRequest
import zio.ZScope.global
import org.scalatra.NotFound

/**
 * REST API for the Worms Server.
 *
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
class PhylogenyApi extends ScalatraServlet:

  get("/names") {
    State.data.map(_.names) match
      case None        => halt(NotFound(ErrorMsg("No data loaded").stringify))
      case Some(names) =>
        names.stringify
  }

  get("/query/startswith/:prefix") {
    val prefix = params("prefix").toLowerCase
    State.data.map(_.names) match
      case None        => halt(NotFound(ErrorMsg("No data loaded").stringify))
      case Some(names) =>
        names.filter(_.toLowerCase.startsWith(prefix)).stringify
  }

  get("/query/contains/:glob") {
    val glob = params("glob").toLowerCase
    State.data.map(_.names) match
      case None        => halt(NotFound(ErrorMsg("No data loaded").stringify))
      case Some(names) =>
        names.filter(_.toLowerCase.contains(glob)).stringify
  }

  get("/tree/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))
    State.data.map(_.findNode(name)) match
      case None      => halt(NotFound(ErrorMsg("No data loaded")))
      case Some(opt) =>
        opt match
          case None       => halt(NotFound(ErrorMsg(s"Unable to find: $name").stringify))
          case Some(node) => node.stringify
  }

  get("/descendants/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))
    State.data.map(_.findNode(name)) match
      case None      => halt(NotFound(ErrorMsg("No data loaded").stringify))
      case Some(opt) =>
        opt match
          case None       => halt(NotFound(ErrorMsg(s"No match for: $name").stringify))
          case Some(node) => node.descendantNames.sorted.stringify

  }

  get("/children/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))
    State.data.map(_.findNode(name)) match
      case None      => halt(NotFound(ErrorMsg("No data loaded").stringify))
      case Some(opt) =>
        opt match
          case None       => halt(NotFound(ErrorMsg(s"No match for: $name").stringify))
          case Some(node) => node.children.map(_.name).sorted.stringify
  }

  get("/parent/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))
    State.data.map(_.parents(name)) match
      case None       => halt(NotFound(ErrorMsg("No data loaded").stringify))
      case Some(node) =>
        node.name.stringify
  }
