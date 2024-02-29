/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import scala.collection.mutable

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
    isMarine: Boolean = false,
    isExtinct: Boolean = false,
)

object WormsConcept:

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
      val wc = WormsConcept(t.id, acceptedId, t.parentId, Seq(WormsConceptName(t.scientificName)), t.rank)
      concepts(t.id) = wc

    for v <- vernacularNames do
      val wc    = concepts(v.id)
      val name  = WormsConceptName(v.vernacularName, false)
      val newWc = wc.copy(names = wc.names :+ name)
      concepts(v.id) = newWc

    for s <- speciesProfiles do
      val wc    = concepts(s.id)
      val newWc = wc.copy(isMarine = s.isMarine, isExtinct = s.isExtinct)
      concepts(s.id) = newWc

    concepts
      .values
      .toSeq
      .sortBy(_.parentId)
