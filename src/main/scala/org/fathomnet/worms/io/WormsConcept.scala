/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import scala.collection.mutable
import org.fathomnet.worms.etc.sdk.Maps
import org.fathomnet.worms.etc.jdk.Logging.given

/*
 * @author Brian Schlining
 * @since 2022-03-17
 */

final case class WormsConceptName(name: String, isPrimary: Boolean = true)

final case class WormsConcept(
    id: Long,
    acceptedId: Long,
    parentId: Option[Long],
    names: Seq[WormsConceptName],
    rank: String,
    isMarine: Option[Boolean] = None,
    isFreshwater: Option[Boolean] = None,
    isTerrestrial: Option[Boolean] = None,
    isExtinct: Option[Boolean] = None,
    isBrackish: Option[Boolean] = None
):
    lazy val primaryName: String =
        names.find(_.isPrimary) match
            case Some(name) => name.name
            case None       => names.head.name

object WormsConcept:

    private val log = System.getLogger(getClass.getName)

    /**
     * Combines the parsed info from the 3 different WorMS files into WormsConcepts
     */
    def build(
        taxons: Seq[Taxon],
        vernacularNames: Seq[VernacularName],
        speciesProfiles: Seq[SpeciesProfile]
    ): Seq[WormsConcept] =
        val concepts = mutable.Map[Long, WormsConcept]()
        for t <- taxons do
            val acceptedId = t.acceptedId.getOrElse(t.id)
            val isPrimary  = t.id == acceptedId
            val name       = WormsConceptName(t.scientificName, isPrimary)
            val wc         = WormsConcept(t.id, acceptedId, t.parentId, Seq(name), t.rank)
            concepts(t.id) = wc

        for v <- vernacularNames do
            val wc    = concepts(v.id)
            val name  = WormsConceptName(v.vernacularName, false)
            val newWc = wc.copy(names = wc.names :+ name)
            concepts(v.id) = newWc

        for s <- speciesProfiles do
            val wc    = concepts(s.id)
            val newWc = wc.copy(
                isMarine = s.isMarine,
                isExtinct = s.isExtinct,
                isBrackish = s.isBrackish,
                isFreshwater = s.isFreshwater,
                isTerrestrial = s.isTerrestrial
            )
            concepts(s.id) = newWc

        concepts.values.toSeq

//        makePrimaryNamesUnique(concepts.values)

    /**
     * WARNING: THIS IS A HACK
     *
     * Given a list of WormsConcepts, make the primary names unique. This is done by appending a number to the name if
     * the name is not unique.
     * @param wormsConcepts
     */
    def makePrimaryNamesUnique(nodes: Iterable[WormsConcept]): Seq[WormsConcept] =
        val map = mutable.Map[String, WormsConcept]()

        def addPossibleDuplicateName(node: WormsConcept): Unit =
            val uniqueName = Maps.findUniqueKey(node.primaryName, map)
            if (node.primaryName != uniqueName) {
                val filteredNames = node.names.filter(_.name != node.primaryName)
                val newNames      = filteredNames :+ WormsConceptName(uniqueName, true)
                val newNode       = node.copy(names = newNames)
                map(uniqueName) = newNode
            }
            else
                map(node.primaryName) = node

        for n <- nodes do
            map.get(n.primaryName) match
                case Some(existing) =>
                    if (existing.id == existing.acceptedId) {
                        if (n.id == n.acceptedId) {
                            log.atWarn.log(
                                s"Duplicate primary name found: ${n.primaryName} (${n.id}) and ${existing.primaryName} (${existing.id})"
                            )

                        }
                        // Change the new name by appending a number
                        addPossibleDuplicateName(n)
                    }
                    else {
                        // TODO change the new name by appending a number
                        map.remove(existing.primaryName)
                        map(n.primaryName) = n
                        addPossibleDuplicateName(existing)
                    }
                case None           =>
                    map(n.primaryName) = n

        map
            .values
            .toSeq
            .sortBy(_.parentId)
