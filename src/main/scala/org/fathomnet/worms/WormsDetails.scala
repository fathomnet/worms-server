/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import org.fathomnet.worms.io.WormsConcept

case class WormsDetails(
    name: String,
    rank: String,
    aphiaId: Long,
    parentAphiaId: Option[Long] = None,
    alternateNames: Seq[String] = Seq.empty,
    isMarine: Option[Boolean] = None,
    isFreshwater: Option[Boolean] = None,
    isTerrestrial: Option[Boolean] = None,
    isExtinct: Option[Boolean] = None,
    isBrackish: Option[Boolean] = None
) {}

object WormsDetails:
    def from(acceptedName: String, wormsConcept: WormsConcept): WormsDetails =
        WormsDetails(
            name = acceptedName,
            rank = wormsConcept.rank,
            aphiaId = wormsConcept.id,
            parentAphiaId = wormsConcept.parentId,
            alternateNames = wormsConcept.names.filterNot(_.isPrimary).map(_.name),
            isMarine = wormsConcept.isMarine,
            isFreshwater = wormsConcept.isFreshwater,
            isTerrestrial = wormsConcept.isTerrestrial,
            isExtinct = wormsConcept.isExtinct,
            isBrackish = wormsConcept.isBrackish
        )
