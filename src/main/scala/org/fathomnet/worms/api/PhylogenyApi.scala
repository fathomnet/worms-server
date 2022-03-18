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
import org.fathomnet.worms.Data
import scala.util.control.NonFatal

/**
 * REST API for the Worms Server.
 *
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
class PhylogenyApi extends ScalatraServlet:


  get("/names") {
    def search(data: Data): String = data.names.stringify
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

  get("/tree/:name") {
    val name = params
      .get("name")
      .getOrElse(halt(BadRequest(ErrorMsg("Please provide a term to look up").stringify)))

    def search(data: Data): String = 
      data.findNodeByName(name) match 
        case None       => halt(NotFound(ErrorMsg(s"Unable to find: `$name`").stringify))
        case Some(node) => node.stringify

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

  private def runSearch[A](search: Data => String) = 
    State.data match
      case None => halt(NotFound(ErrorMsg("The WoRMS dataset is missing. If this continues, please report it to the FathomNet team.").stringify))
      case Some(data) => search(data)

