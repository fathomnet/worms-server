/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms

import org.fathomnet.worms.io.WormsTreeNode

// import com.github.plokhotnyuk.jsoniter_scala.macros._
// import com.github.plokhotnyuk.jsoniter_scala.core._

// given codec: JsonValueCodec[WormsNode] = JsonCodecMaker.make

// Immutable tree node
final case class WormsNode(
    name: String,
    rank: String,
    alternateNames: Seq[String],
    children: Seq[WormsNode]
):

  def find(name: String): Option[WormsNode] =
    if (this.name == name || this.alternateNames.contains(name))
      Some(this)
    else
      this.children.foldLeft(None: Option[WormsNode]) { (acc, child) =>
        acc match
          case Some(node) => Some(node)
          case None       => child.find(name)
      }

  def descendantNames: Seq[String] =
    this.children.foldLeft(Seq(this.name): Seq[String]) { (acc, child) =>
      acc ++ child.descendantNames
    }

  def names: Seq[String] = 
    (this.name +: this.alternateNames).toSeq.sorted

object WormsNodeBuilder:

  def from(node: WormsTreeNode): WormsNode =
    val name           = node
      .concept
      .names
      .find(_.isPrimary)
      .map(_.name)
      .get
    val alternateNames = node
      .concept
      .names
      .filter(_.isPrimary == false)
      .map(_.name)
    WormsNode(name, node.concept.rank, alternateNames, node.children.map(from).toSeq)
