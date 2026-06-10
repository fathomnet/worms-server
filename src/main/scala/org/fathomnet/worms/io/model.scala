/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import scala.io.Source
import scala.util.{Success, Try, Using}

/*
 * This file contains code to read the 3 relevant files from the Worms
 * download.
 *
 * @author Brian Schlining
 * @since 2022-03-17
 */

def taxonIDToKey(taxonID: String): Long =
    taxonID.split(":").last.toLong

/**
  * Helper function to get a column from a row of the Worms download, 
  * handling quoted values and missing values.
  *
  * @param i THe index of the column to get
  * @param cols The columns of the row, passed as an implicit parameter
  * @return The value of the column, with quotes removed and empty values returned as an empty string
  */
def get(i: Int)(using cols: List[String]): String =
    val a = cols.applyOrElse(i, (_: Int) => "")
    val b = if a.startsWith("\"") then a.substring(1, a.length - 1) else a
    if b.endsWith("\"") then b.substring(0, b.length - 1) else b

def readFile[A](file: String, rowMapper: String => Option[A]): List[A] =
    Using(Source.fromFile(file)) { source =>
        source.getLines().flatMap(rowMapper).toList
    } match
        case Success(list) => list
        case _             => List.empty[A]

final case class Taxon(
    taxonID: String,
    parentNameUsageID: Option[String],
    scientificName: String,
    rank: String,
    acceptedNameUsageID: Option[String]
):
    val id: Long                 = taxonIDToKey(taxonID)
    val parentId: Option[Long]   = parentNameUsageID.map(taxonIDToKey)
    val acceptedId: Option[Long] = acceptedNameUsageID.map(taxonIDToKey)

object Taxon:
    def from(row: String): Option[Taxon] =
        Try {
            given cols: List[String] = row.split("\t", -1).toList
            val taxonID             = get(0)
            val acceptedNameUsageID = if get(2).isBlank then None else Some(get(2))
            val parentNameUsageID   = if get(3).isBlank then None else Some(get(3))
            val scientificName      = get(5)
            val rank                = get(19)
            Taxon(taxonID, parentNameUsageID, scientificName, rank, acceptedNameUsageID)
        }.toOption

    def read(file: String): List[Taxon] = readFile(file, Taxon.from)

final case class VernacularName(taxonID: String, vernacularName: String):
    val id = taxonIDToKey(taxonID)

object VernacularName:
    def from(row: String): Option[VernacularName] =
        Try {
            given cols: List[String] = row.split("\t", -1).toList
            VernacularName(get(0), get(1))
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
    val id: Long = taxonIDToKey(taxonID)

object SpeciesProfile:

    private def toBool(value: String): Option[Boolean] =
        if value.isBlank then None
        else Some(value == "1")

    def from(row: String): Option[SpeciesProfile] =
        Try {
            given cols: List[String] = row.split("\t", -1).toList
            SpeciesProfile(get(0), toBool(get(1)), toBool(get(2)), toBool(get(3)), toBool(get(4)), toBool(get(5)))
        }.toOption

    def read(file: String): List[SpeciesProfile] = readFile(file, SpeciesProfile.from)
