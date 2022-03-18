/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.io

import scala.util.Try
import scala.io.Source

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
    rank: String
):
  val id       = taxonIDToKey(taxonID)
  val parentId = parentNameUsageID.map(taxonIDToKey)

object Taxon:
  def from(row: String): Option[Taxon] =
    Try {
      val cols              = row.split("\t")
      val parentNameUsageID = if cols(3).isBlank then None else Some(cols(3))
      Taxon(cols(0), parentNameUsageID, cols(5), cols(19))
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

final case class SpeciesProfile(taxonID: String, isMarine: Boolean, isExtinct: Boolean):
  val id = taxonIDToKey(taxonID)

object SpeciesProfile:
  def from(row: String): Option[SpeciesProfile] =
    Try {
      val cols = row.split("\t")
      SpeciesProfile(cols(0), cols(1) == "1", cols(4) == "1")
    }.toOption

  def read(file: String): List[SpeciesProfile] = readFile(file, SpeciesProfile.from)
