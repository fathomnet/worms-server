/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import scala.collection.immutable.SortedMap
import org.fathomnet.worms.io.WormsConcept
import scala.annotation.tailrec

/**
 * Wraps a Worms tree with untility methods for fast access.
 * @param rootNode
 *   The root node of the WoRMS phylogenetic tree.
 *
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
final case class Data(rootNode: WormsNode, wormsConcepts: Seq[WormsConcept]):

    private val log = System.getLogger(getClass.getName)

    /**
     * Map of [nodeName, Node] for both the name and alternate name of the node.
     */
    lazy val namesMap: SortedMap[String, WormsNode] =
        val map                        = SortedMap.newBuilder[String, WormsNode]
        def add(node: WormsNode): Unit =
            map += node.name -> node
            node.alternateNames.foreach(n => map += n -> node)
            node.children.foreach(add)
        add(rootNode)
        map.result()

    /**
     * All names used in WoRMS.
     */
    lazy val names: Set[String] = namesMap.keySet

    /**
     * Map of [childNodeName: String, parentNode: WormsNode] for both the name and alternate name of the node.
     */
    lazy val parents: SortedMap[String, WormsNode] =
        val map                        = SortedMap.newBuilder[String, WormsNode]
        val visited = scala.collection.mutable.Set.empty[String] // avoids cyclic relations in the tree

        def add(node: WormsNode): Unit =
            if visited.add(node.name) then
                node.children.foreach(n => map += n.name -> node)
                node.children.foreach(n => n.alternateNames.foreach(n => map += n -> node))
                node.children.foreach(add)
            else
                log.log(
                    System.Logger.Level.WARNING,
                    s"Node ${node.name} already visited, skipping to avoid cycles."
                )
        add(rootNode)
        map.result()

    def findNodeByName(name: String): Option[WormsNode] = namesMap.get(name)

    def findNodeByChildName(name: String): Option[WormsNode] = parents.get(name)

    
    def buildParentPath(name: String): List[WormsNode] =
        def build(node: WormsNode, visited: Set[String] = Set.empty): List[WormsNode] =
            if visited.contains(node.name) then
                // We've detected a cycle, stop recursion
                log.log(System.Logger.Level.WARNING, s"Cycle detected at node ${node.name}, stopping recursion.")
                List(node)
            else
                findNodeByChildName(node.name) match
                    case Some(parent) => parent :: build(parent, visited + node.name)
                    case None         => List(node)
        findNodeByName(name) match
            case None       => List.empty
            case Some(node) => build(node).reverse.tail :+ node

    def buildParentTree(name: String): Option[WormsNode] =
        val nodes = buildParentPath(name)
        nodes.foldRight(Option.empty[WormsNode]):
            case (node, None)          => Some(node.copy(children = Nil))
            case (parent, Some(child)) =>
                val newParent = parent.copy(children = Seq(child))
                Some(newParent)
