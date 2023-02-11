/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import java.nio.file.Path
import zio.ZIO
import scala.util.Try
import org.fathomnet.worms.etc.jdk.Logging.given
import org.fathomnet.worms.etc.zio.ZioUtil
import org.fathomnet.worms.{WormsNode, WormsNodeBuilder}
import scala.concurrent.ExecutionContext

/**
 * Loades a Worm download into memory
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
object WormsLoader:

  private val log = System.getLogger(getClass.getName)

  /**
   * Loads the Worms download into memory from files in a directory
   * @param wormsDir
   *   The directory containing the Worms download
   */
  def load(wormsDir: Path)(using ec: ExecutionContext): Option[WormsNode] =
    val taxonPath          = wormsDir.resolve("taxon.txt")
    val vernacularNamePath = wormsDir.resolve("vernacularname.txt")
    val speciesProfilePath = wormsDir.resolve("speciesprofile.txt")

    val app = for
      _               <- ZIO.succeed(log.atInfo.log(s"Loading WoRMS from $wormsDir"))
      taxons          <- ZIO.fromTry(Try(Taxon.read(taxonPath.toString)))
      vernacularNames <- ZIO.fromTry(Try(VernacularName.read(vernacularNamePath.toString)))
      speciesProfiles <- ZIO.fromTry(Try(SpeciesProfile.read(speciesProfilePath.toString)))
      wormsConcepts   <-
        ZIO.fromTry(Try(WormsConcept.build(taxons, vernacularNames, speciesProfiles).toList))
      mutableRoot     <- ZIO.fromTry(Try(MutableWormsNodeBuilder.fathomNetTree(wormsConcepts)))
      root            <- ZIO.fromTry(Try(WormsNodeBuilder.from(mutableRoot)))
      _               <- ZIO.succeed(log.atInfo.log(s"Loaded WoRMS from $wormsDir"))
    yield Some(root)

    ZioUtil.safeRun(app).getOrElse(None)

 
