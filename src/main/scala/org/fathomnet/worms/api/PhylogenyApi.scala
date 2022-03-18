/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.api

import org.scalatra.ScalatraServlet
import org.fathomnet.worms.{ErrorMsg, State}
import org.fathomnet.worms.etc.circe.CirceCodecs.{given, *}
import org.scalatra.BadRequest
import zio.ZScope.global
import org.scalatra.NotFound

class PhylogenyApi extends ScalatraServlet:

  get("/names") {
      State.data.map(_.names) match 
        case None => halt(NotFound(ErrorMsg("No data loaded")))
        case Some(names) =>
          names.stringify
  }

  get("/startswith/:prefix") {
    val prefix = params("prefix").toLowerCase
    State.data.map(_.names) match 
      case None => halt(NotFound(ErrorMsg("No data loaded")))
      case Some(names) =>
        names.filter(_.toLowerCase.startsWith(prefix)).stringify
  }

  get("/contains/:glob") {
    val glob = params("glob").toLowerCase
    State.data.map(_.names) match 
      case None => halt(NotFound(ErrorMsg("No data loaded")))
      case Some(names) =>
        names.filter(_.toLowerCase.contains(glob)).stringify
  }

  get("/tree/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))
    State.data.map(_.findNode(name)) match
      case None => halt(NotFound(ErrorMsg("No data loaded")))
      case Some(node) =>
        node.stringify
  }

  get("/descendants/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))
    State.data.flatMap(_.findNode(name)) match
      case None => halt(NotFound(ErrorMsg("No data loaded")))
      case Some(node) =>
        node.descendantNames.stringify
  }

  get("/parent/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))
    State.data.map(_.parents(name)) match
      case None => halt(NotFound(ErrorMsg("No data loaded")))
      case Some(node) =>
        node.name.stringify
  }
