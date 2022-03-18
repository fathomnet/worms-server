/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.io

import scala.collection.mutable

case class WormsTreeNode(
    concept: WormsConcept,
    children: mutable.ArrayBuffer[WormsTreeNode] = mutable.ArrayBuffer.empty
)

object WormsTree:

  def fathomNetTree(wormsConcepts: Seq[WormsConcept]): WormsTreeNode =
    trimTree(buildTree(filterFlattenedTree(wormsConcepts)))

  def buildTree(wormsConcepts: Seq[WormsConcept]): WormsTreeNode =
    val map = mutable.Map[Long, WormsTreeNode]()
    wormsConcepts.foreach(c => map.put(c.id, WormsTreeNode(c)))
    for
      c           <- wormsConcepts
      conceptNode <- map.get(c.id)
      parentId    <- c.parentId
      parentNode  <- map.get(parentId)
    do parentNode.children.append(conceptNode)
    map.get(1L).get

  def trimTree(rootNode: WormsTreeNode): WormsTreeNode =
    def filter(children: Seq[WormsTreeNode]): Seq[WormsTreeNode] =
      children.filter(_.concept.names.map(_.name).exists(_.contains("Animalia")))

    val accepted = filter(rootNode.children.toSeq)
    if (accepted.isEmpty) rootNode
    else WormsTreeNode(rootNode.concept, mutable.ArrayBuffer().appendAll(accepted))

  def flattenTree(node: WormsTreeNode): Seq[WormsConcept] =
    node.concept +: node.children.toSeq.flatMap(flattenTree)

  def filterFlattenedTree(rows: Seq[WormsConcept]): Seq[WormsConcept] =
    rows.filter(wc =>
      val lr = wc.rank.toLowerCase
      (!lr.contains("species") && !lr.contains("genus") && !lr.contains(
        "variety"
      )) || (!wc.isExtinct && wc.isMarine)
    )

  def printTree(node: WormsTreeNode, indent: Int = 0): Unit =
    println(s"${"  " * indent}${node.concept.names.map(_.name).mkString(", ")}")
    node.children.foreach(printTree(_, indent + 1))
