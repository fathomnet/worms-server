/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import org.fathomnet.worms.io.MutableWormsNode

/**
 * Immutable tree node
 *
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
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

  lazy val descendantNames: Seq[String] =
    this.children.foldLeft(Seq(this.name): Seq[String]) { (acc, child) =>
      acc ++ child.descendantNames
    }

  lazy val names: Seq[String] =
    (this.name +: this.alternateNames.sorted).toSeq

  lazy val simple: SimpleWormsNode = SimpleWormsNode(
    name = this.name,
    rank = this.rank,
    alternateNames = this.alternateNames
  )

object WormsNodeBuilder:

  def from(node: MutableWormsNode): WormsNode =
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

final case class SimpleWormsNode(name: String, rank: String, alternateNames: Seq[String])
