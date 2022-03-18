/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.util

object HexUtil:

  def toHex(bytes: Array[Byte]): String =
    val sb = new StringBuilder
    for (b <- bytes)
      sb.append(String.format("%02x", Byte.box(b)))
    sb.toString

  def fromHex(hex: String): Array[Byte] =
    hex
      .grouped(2)
      .toArray
      .map(i => Integer.parseInt(i, 16).toByte)
