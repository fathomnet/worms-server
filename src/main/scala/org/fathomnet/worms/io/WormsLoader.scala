/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import org.fathomnet.worms.etc.jdk.Logging.given
import org.fathomnet.worms.etc.zio.ZioUtil
import org.fathomnet.worms.{WormsNode, WormsNodeBuilder}
import zio.ZIO

import java.nio.file.Path
import scala.concurrent.ExecutionContext
import scala.util.Try

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
    def load(wormsDir: Path)(using ec: ExecutionContext): (Seq[WormsConcept], Option[WormsNode]) =
        val taxonPath          = wormsDir.resolve("taxon.txt")
        val vernacularNamePath = wormsDir.resolve("vernacularname.txt")
        val speciesProfilePath = wormsDir.resolve("speciesprofile.txt")

        val app = for
            _                      <- ZIO.succeed(log.atInfo.log(s"Loading WoRMS from $wormsDir"))
            taxons                 <- ZIO.fromTry(Try(Taxon.read(taxonPath.toString)))
            vernacularNames        <- ZIO.fromTry(Try(VernacularName.read(vernacularNamePath.toString)))
            speciesProfiles        <- ZIO.fromTry(Try(SpeciesProfile.read(speciesProfilePath.toString)))
            wormsConcepts          <-
                ZIO.fromTry(Try(WormsConcept.build(taxons, vernacularNames, speciesProfiles).toList))
            mutableRoot            <- ZIO.fromTry(Try(MutableWormsNodeBuilder.fathomNetTree(wormsConcepts)))
            fathomnetWormsConcepts <- ZIO.succeed(MutableWormsNodeBuilder.flattenTree(mutableRoot))
            renamedWormsConcepts   <- ZIO.succeed(WormsConcept.makePrimaryNamesUnique(fathomnetWormsConcepts))
            fathomnetRoot          <- ZIO.fromTry(Try(MutableWormsNodeBuilder.buildTree(renamedWormsConcepts)))
            root                   <- ZIO.fromTry(Try(WormsNodeBuilder.from(fathomnetRoot)))
            _                      <- ZIO.succeed(log.atInfo.log(s"Loaded WoRMS from $wormsDir"))
        yield (wormsConcepts, Some(root))

        ZioUtil.safeRun(app).getOrElse((Seq.empty, None))
