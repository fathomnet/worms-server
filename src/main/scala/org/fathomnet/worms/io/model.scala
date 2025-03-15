/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import scala.io.Source
import scala.util.Try

/*
 * This file contains code to read the 3 relevant files from the Worms
 * download.
 *
 * @author Brian Schlining
 * @since 2022-03-17
 */

def taxonIDToKey(taxonID: String): Long =
    taxonID.split(":").last.toLong

def readFile[A](file: String, rowMapper: String => Option[A]): List[A] =
    Source
        .fromFile(file)
        .getLines
        .flatMap(rowMapper)
        .toList

final case class Taxon(
    taxonID: String,
    parentNameUsageID: Option[String],
    scientificName: String,
    rank: String,
    acceptedNameUsageID: Option[String]
):
    val id         = taxonIDToKey(taxonID)
    val parentId   = parentNameUsageID.map(taxonIDToKey)
    val acceptedId = acceptedNameUsageID.map(taxonIDToKey)

object Taxon:
    def from(row: String): Option[Taxon] =
        Try {
            val cols                = row.split("\t")
            val taxonID             = cols(0)
            val acceptedNameUsageID = if cols(2).isBlank then None else Some(cols(2))
            val parentNameUsageID   = if cols(3).isBlank then None else Some(cols(3))
            val scientificName      = cols(5)
            val rank                = cols(19)
            Taxon(taxonID, parentNameUsageID, scientificName, rank, acceptedNameUsageID)
        }.toOption

    def read(file: String): List[Taxon] = readFile(file, Taxon.from)

final case class VernacularName(taxonID: String, vernacularName: String):
    val id = taxonIDToKey(taxonID)

object VernacularName:
    def from(row: String): Option[VernacularName] =
        Try {
            val cols = row.split("\t")
            VernacularName(cols(0), cols(1))
        }.toOption

    def read(file: String): List[VernacularName] = readFile(file, VernacularName.from)

final case class SpeciesProfile(
    taxonID: String,
    isMarine: Option[Boolean],
    isFreshwater: Option[Boolean],
    isTerrestrial: Option[Boolean],
    isExtinct: Option[Boolean],
    isBrackish: Option[Boolean]
):
    val id = taxonIDToKey(taxonID)

object SpeciesProfile:

    private def toBool(value: String): Option[Boolean] =
        if value.isBlank then None
        else Some(value == "1")

    def from(row: String): Option[SpeciesProfile] =
        Try {
            val cols = row.split("\t")
            SpeciesProfile(cols(0), toBool(cols(1)), toBool(cols(2)), toBool(cols(3)), toBool(cols(4)), toBool(cols(5)))
        }.toOption

    def read(file: String): List[SpeciesProfile] = readFile(file, SpeciesProfile.from)
