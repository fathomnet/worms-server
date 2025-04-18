/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.etc.circe

import io.circe.*
import io.circe.generic.semiauto.*
import org.fathomnet.worms.util.HexUtil
import org.fathomnet.worms.{ErrorMsg, Names, Page, SimpleWormsNode, WormsNode}

import java.net.{URI, URL}
import scala.util.Try
import org.fathomnet.worms.WormsDetails

/**
 * JSON codecs for use with Circe. Usage:
 * {{{
 * import org.fathomnet.worms.etc.circe.CirceCodecs.{given, *}
 * val someObj = ...
 * val json = someObj.stringify
 * }}}
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
object CirceCodecs:

    given byteArrayEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]]:
        final def apply(xs: Array[Byte]): Json =
            Json.fromString(HexUtil.toHex(xs))
    given byteArrayDecoder: Decoder[Array[Byte]] = Decoder
        .decodeString
        .emapTry(str => Try(HexUtil.fromHex(str)))

    given urlDecoder: Decoder[URL] = Decoder
        .decodeString
        .emapTry(str => Try(URI.create(str).toURL()))
    given urlEncoder: Encoder[URL] = Encoder
        .encodeString
        .contramap(_.toString)

    given Decoder[WormsDetails] = deriveDecoder
    given Encoder[WormsDetails] = deriveEncoder

    given Decoder[WormsNode] = deriveDecoder
    given Encoder[WormsNode] = deriveEncoder

    given Decoder[SimpleWormsNode] = deriveDecoder
    given Encoder[SimpleWormsNode] = deriveEncoder

    given Decoder[Names] = deriveDecoder
    given Encoder[Names] = deriveEncoder

    given Decoder[ErrorMsg] = deriveDecoder
    given Encoder[ErrorMsg] = deriveEncoder

    given Decoder[Page[String]] = deriveDecoder
    given Encoder[Page[String]] = deriveEncoder

    private val printer = Printer.noSpaces.copy(dropNullValues = true)

    /**
     * Convert a circe Json object to a JSON string
     * @param value
     *   Any value with an implicit circe coder in scope
     */
    extension (json: Json) def stringify: String = printer.print(json)

    /**
     * Convert an object to a JSON string
     * @param value
     *   Any value with an implicit circe coder in scope
     */
    extension [T: Encoder](value: T) def stringify: String = Encoder[T].apply(value).stringify
