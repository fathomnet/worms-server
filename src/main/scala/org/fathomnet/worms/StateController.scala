/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import scala.util.control.NonFatal
import org.fathomnet.worms.etc.jdk.Logging.given
import scala.collection.mutable.ListMap

object StateController:

  private val log = System.getLogger(getClass.getName)

  def runSearch[A](search: Data => A): Either[ErrorMsg, A] =
    State.data match
      case None       =>
        Left(
          NotFound(
            "The WoRMS dataset is missing; this may happen for a few seconds when the server starts. If this continues, please report it to the FathomNet team."
          )
        )
      case Some(data) =>
        try Right(search(data))
        catch
          case NonFatal(e) =>
            log.atWarn.withCause(e).log("Error while running a search")

            Left(
              ServerError(
                s"An unexpected error occurred: ${e.getMessage}. Did you ask for the entire WoRMS dataset in one request? Maybe don't do that."
              )
            )

  def runNodeSearch[A](search: Data => Option[A], notFoundMsg: String): Either[ErrorMsg, A] =
    runSearch(search).fold(
      e => Left(e),
      node =>
        node match
          case Some(n) => Right(n)
          case None    => Left(NotFound(notFoundMsg))
    )

  def findAllNames(limit: Int, offset: Int): Either[ErrorMsg, Page[String]] =
    def search(data: Data): Page[String] = //data.names.stringify
      val names = data.names.slice(offset, offset + limit).toSeq
      Page(names, limit, offset, data.names.size)
    runSearch(search)

  def countAllNames(): Either[ErrorMsg, Int] =
    runSearch(data => data.names.size)

  def queryNamesStartingWith(prefix: String): Either[ErrorMsg, List[String]] =
    def search(data: Data): List[String] =
      data.names.filter(_.toLowerCase.startsWith(prefix.toLowerCase)).toList
    runSearch(search)

  def queryNamesContaining(glob: String): Either[ErrorMsg, List[String]] =
    def search(data: Data): List[String] =
      data.names.filter(_.toLowerCase.contains(glob.toLowerCase)).toList
    runSearch(search)

  def descendantNames(name: String): Either[ErrorMsg, List[String]] =
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

  def ancestorNames(name: String): Either[ErrorMsg, List[String]] =
    def search(data: Data): List[String] =
      data.buildParentPath(name).map(_.name)
    runSearch(search)

  def childNames(name: String): Either[ErrorMsg, List[String]] =
    def search(data: Data): List[String] =
      data.findNodeByName(name) match
        case None       => Nil
        case Some(node) => node.children.map(_.name).sorted.toList
    runSearch(search).fold(
      e => Left(e),
      v => if (v.isEmpty) Left(NotFound(s"Unable to find `$name`")) else Right(v)
    )

  def parentName(name: String): Either[ErrorMsg, String] =
    def search(data: Data): Option[String] =
      data.findNodeByChildName(name).map(_.name)
    runSearch(search).fold(
      e => Left(e),
      v =>
        v match
          case None    => Left(NotFound(s"Unable to find `$name`"))
          case Some(p) => Right(p)
    )

  def synonyms(name: String): Either[ErrorMsg, List[String]] =
    def search(data: Data): List[String] =
      data.findNodeByName(name) match
        case None       => Nil
        case Some(node) => node.names.toList

    runSearch(search).fold(
      e => Left(e),
      v => if (v.isEmpty) Left(NotFound(s"Unable to find `$name`")) else Right(v)
    )

  def descendantTaxa(name: String): Either[ErrorMsg, WormsNode] =
    def search(data: Data): Option[WormsNode] =
      data.findNodeByName(name)
    runNodeSearch(search, s"Unable to find `$name`")

  def ancestorTaxa(name: String): Either[ErrorMsg, WormsNode] =
    def search(data: Data): Option[WormsNode] =
      data.buildParentTree(name)
    runNodeSearch(search, s"Unable to find `$name`")

  def taxaInfo(name: String): Either[ErrorMsg, SimpleWormsNode] =
    def search(data: Data): Option[SimpleWormsNode] =
      data.findNodeByName(name).map(_.simple)
    runNodeSearch(search, s"Unable to find `$name`")

  def parentTaxa(name: String): Either[ErrorMsg, SimpleWormsNode] =
    def search(data: Data): Option[SimpleWormsNode] =
      data.findNodeByChildName(name).map(_.simple)
    runNodeSearch(search, s"Unable to find `$name`")

  def childTaxa(name: String): Either[ErrorMsg, List[SimpleWormsNode]] =
    def search(data: Data): Option[WormsNode] =
      data.findNodeByName(name)
    runNodeSearch(search, s"Unable to find `$name`")
      .map(_.children.map(_.simple).sortBy(_.name).toList)


