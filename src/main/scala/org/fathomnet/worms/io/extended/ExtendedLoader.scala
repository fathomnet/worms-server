/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io.extended

import scala.concurrent.ExecutionContext
import org.fathomnet.worms.WormsNode
import java.nio.file.Path
import org.fathomnet.worms.io.{WormsConcept, WormsConceptName}
import scala.util.control.NonFatal
import org.fathomnet.worms.io.MutableWormsNodeBuilder
import org.fathomnet.worms.WormsNodeBuilder
import org.fathomnet.worms.etc.jdk.Logging.given
import scala.util.Using
import sttp.tapir.EndpointIO.annotations.formBody

object ExtendedLoader:

    private val log = System.getLogger(getClass.getName)

    /**
     * Load a CSV likd:
     * ```csv
     * id,parentId,names
     * 1000,,equipment
     * 1001,1000,platform
     * 1002,1000,Clathrate Bucket
     * 1003,1000,Benthic Instrument Node;BIN
     * 1004,1000,TPC;temperature, pressure, conductivity sensor
     * 1005,1000,Wax corer
     * 1006,1000,site marker
     * 1007,1000,Dissolution Ball
     * 1008,1000,Odor Pump
     * 1009,1000,Remote Instrument Node;RIN
     * ```
     *
     * @param extendedFile
     * @param ec
     * @return
     */
    def load(extendedFile: Path): Option[WormsNode] =
        val wormsConcepts = read(extendedFile.toString)
        val tree          = MutableWormsNodeBuilder.buildTree(wormsConcepts)
        Option(WormsNodeBuilder.from(tree))

    def read(file: String): Seq[WormsConcept] =
        val t = Using(scala.io.Source.fromFile(file)) { source =>
            source.getLines().flatMap(from).toSeq
        }
        t match
            case scala.util.Success(s) => s
            case scala.util.Failure(e) =>
                log.atError.withCause(e).log(s"Failed to read file: $file")
                Seq.empty

    def from(row: String): Option[WormsConcept] =
        try
            val cols         = row.split(",").map(_.trim)
            val parentId     = if cols(1).isBlank() then None else Some(cols(1).toLong)
            val names        = cols(2)
                .split(";")
                .map(_.trim)
            val conceptNames =
                WormsConceptName(names.head, true) +: names.tail.map(WormsConceptName(_, false))
            val aphiaId      = cols(0).toLong
            Some(WormsConcept(aphiaId, aphiaId, parentId, conceptNames.toIndexedSeq, ""))
        catch
            case NonFatal(e) =>
                log.atWarn.withCause(e).log(s"Failed to parse row: $row")
                None
