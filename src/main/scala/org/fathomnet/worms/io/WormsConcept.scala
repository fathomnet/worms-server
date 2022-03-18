/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.io

import scala.collection.mutable

final case class WormsConceptName(name: String, isPrimary: Boolean = true)

final case class WormsConcept(
    id: Long,
    parentId: Option[Long],
    names: Seq[WormsConceptName],
    rank: String,
    isMarine: Boolean = false,
    isExtinct: Boolean = false
)

object WormsConcept:

  def build(
      taxons: Seq[Taxon],
      vernacularNames: Seq[VernacularName],
      speciesProfiles: Seq[SpeciesProfile]
  ): Seq[WormsConcept] =
    val concepts = mutable.Map[Long, WormsConcept]()
    for t <- taxons do
      val wc = WormsConcept(t.id, t.parentId, Seq(WormsConceptName(t.scientificName)), t.rank)
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
