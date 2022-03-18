/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.etc.circe

import io.circe._
import io.circe.generic.semiauto._
import scala.util.Try
import java.net.URL
import org.fathomnet.worms.util.HexUtil
import org.fathomnet.worms.{ErrorMsg, WormsNode}

object CirceCodecs:

  given byteArrayEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]]:
    final def apply(xs: Array[Byte]): Json =
      Json.fromString(HexUtil.toHex(xs))
  given byteArrayDecoder: Decoder[Array[Byte]] = Decoder
    .decodeString
    .emapTry(str => Try(HexUtil.fromHex(str)))

  given urlDecoder: Decoder[URL] = Decoder
    .decodeString
    .emapTry(str => Try(new URL(str)))
  given urlEncoder: Encoder[URL] = Encoder
    .encodeString
    .contramap(_.toString)

  given Decoder[WormsNode] = deriveDecoder
  given Encoder[WormsNode] = deriveEncoder

  given Decoder[ErrorMsg] = deriveDecoder
  given Encoder[ErrorMsg] = deriveEncoder

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
