/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.io

import java.nio.file.Path
import zio.ZIO
import scala.util.Try
import org.fathomnet.worms.etc.jdk.Logging.given
import org.fathomnet.worms.{WormsNode, WormsNodeBuilder}

object WormsLoader:

  private val log = System.getLogger(getClass.getName)

  def load(wormsDir: Path): Option[WormsNode] =
    // def run(wormsDir: Path): Int =
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
      rootNode        <- ZIO.fromTry(Try(WormsTree.fathomNetTree(wormsConcepts)))
      wormsNode       <- ZIO.fromTry(Try(WormsNodeBuilder.from(rootNode)))
      _               <- ZIO.succeed(log.atInfo.log(s"Loaded WoRMS from $wormsDir"))
    yield Some(wormsNode)

    val runtime = zio.Runtime.default
    runtime.unsafeRun(
      app.catchAll(t =>
        ZIO.succeed {
          log.atError.withCause(t).log(t.getMessage)
          None
        }
      )
    )
