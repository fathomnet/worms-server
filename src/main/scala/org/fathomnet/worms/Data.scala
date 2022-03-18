/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms

import org.fathomnet.worms.io.WormsTreeNode
import scala.collection.immutable.SortedMap

final case class Data(rootNode: WormsNode):

  /**
   * Map of [nodeName: String, Node] for both the name and alternate name of the node.
   */
  lazy val namesMap: SortedMap[String, WormsNode] =
    val map = SortedMap.newBuilder[String, WormsNode]
    def add(node: WormsNode): Unit =
      map += node.name -> node
      node.alternateNames.foreach(n => map += n -> node)
      node.children.foreach(add)
    add(rootNode)
    map.result()

  lazy val names: Set[String] = namesMap.keySet

  /**
   * Map of [childNodeName: String, parentNode: WormsNode] for both the name and alternate name of the node.
   */
  lazy val parents: SortedMap[String, WormsNode] =
    val map = SortedMap.newBuilder[String, WormsNode]
    def add(node: WormsNode): Unit =
      node.children.foreach(n => map += n.name -> node)
      node.children.foreach(add)
    add(rootNode)
    map.result()

  def findNode(name: String): Option[WormsNode] = namesMap.get(name)


  

