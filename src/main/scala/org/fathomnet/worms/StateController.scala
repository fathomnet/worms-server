/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import org.fathomnet.worms.etc.jdk.Logging.given

import scala.util.control.NonFatal
import org.fathomnet.worms.State.data

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
        def search(data: Data): Page[String] = // data.names.stringify
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

    def findNamesByAphiaId(aphiaId: Long): Either[ErrorMsg, Names] =
        def search(data: Data): Option[Names] =
            val allNodes  = data.namesMap.values
            val existing  = allNodes.find(n => n.aphiaId == aphiaId)
            val outdated  = allNodes.filter(n => n.acceptedAphiaId == aphiaId)
            val candidate = existing match
                case None       => None
                case Some(node) =>
                    val accepted =
                        if (node.aphiaId == node.acceptedAphiaId)
                            Some(node)
                        else
                            allNodes.find(n => n.aphiaId == node.acceptedAphiaId)
                    val names    = accepted match
                        case None        => Names(node.aphiaId, node.name, node.name, node.alternateNames)
                        case Some(value) =>
                            val alternateNames = (node.alternateNames ++ value.alternateNames)
                                .toSet
                                .toSeq
                                .sorted
                            Names(node.aphiaId, node.name, value.name, alternateNames)

                    Option(names)

            if (outdated.isEmpty) candidate
            else
                candidate.map(c =>
                    val alternateNames = (outdated.flatMap(n => n.names) ++ c.alternateNames)
                        .filter(s => s != c.acceptedName)
                        .toSet
                        .toSeq
                        .sorted
                    c.copy(alternateNames = alternateNames)
                )

        runNodeSearch(search, s"Unable to find a name with aphiaId: $aphiaId")

    def descendantNames(name: String, acceptedOnly: Boolean = false): Either[ErrorMsg, List[String]] =
        def search(data: Data): List[String] =
            data.findNodeByName(name) match
                case None       => Nil
                case Some(node) =>
                    if (acceptedOnly && node.isAccepted)
                        node.descendants.filter(_.isAccepted).map(_.name).sorted.toList
                    else
                        node.descendantNames.sorted.toList
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
        for
            node  <- taxaInfo(name)
            names <- findNamesByAphiaId(node.acceptedAphiaId)
        yield
            val alternativeNames =
                if (names.acceptedName == name)
                    names.alternateNames.toList
                else
                    names.name :: names.alternateNames.toList

            names.acceptedName +: alternativeNames.filter(s => s != names.acceptedName).distinct.sorted

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

    def taxaByNameStartingWith(
        prefix: String,
        rankOpt: Option[String] = None,
        parentOpt: Option[String] = None
    ): Either[ErrorMsg, List[SimpleWormsNode]] =

        def search(data: Data): List[SimpleWormsNode] =

            val candidateNodes = parentOpt match
                case None         =>
                    // Fast path
                    data
                        .names
                        .filter(_.toLowerCase.startsWith(prefix.toLowerCase))
                        .flatMap(data.findNodeByName)
                        .map(_.simple)
                        .toList
                case Some(parent) =>
                    data.findNodeByName(parent) match
                        case None             => Nil
                        case Some(parentNode) =>
                            parentNode
                                .descendants
                                .filter(node => node.names.map(_.toLowerCase).exists(_.startsWith(prefix.toLowerCase)))
                                .map(_.simple)
                                .toList

            rankOpt match
                case None       => candidateNodes
                case Some(rank) =>
                    val lowerCaseRank = rank.toLowerCase
                    candidateNodes.filter(_.rank.toLowerCase == lowerCaseRank)

            // ORIGINAL CODE
            // data
            //   .names
            //   .filter(_.toLowerCase.startsWith(prefix.toLowerCase))
            //   .flatMap(data.findNodeByName)
            //   .map(_.simple)
            //   .toList
        runSearch(search)

    def details(name: String): Either[ErrorMsg, WormsDetails] =
        for 
            data         <- State.data
                                .toRight(NotFound("The WoRMS dataset is missing; this may happen for a few seconds when the server starts. If this continues, please report it to the FathomNet team."))
            names        <- synonyms(name)
            acceptedName <- names.headOption.toRight(NotFound(s"Unable to find `$name`"))
            wormsConcept <- data.wormsConcepts.find(_.names.exists(_.name == acceptedName)).toRight(NotFound(s"Unable to find `$name` accepted name of `$acceptedName`"))
        yield

            WormsDetails.from(acceptedName, wormsConcept)
                .copy(alternateNames = names.tail)
            
