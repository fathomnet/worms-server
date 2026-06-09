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
     * Multimap of [name, nodes] for both the primary name and alternate names of each node.
     * A single name can map to multiple nodes when common/vernacular names are shared across
     * different taxa (e.g. "limpets" appears as a vernacular name for both Patellidae and
     * Acmaeidae). Storing all matching nodes prevents the last-writer-wins silent data loss
     * that occurred with the previous SortedMap[String, WormsNode].
     */
    lazy val namesMap: SortedMap[String, Vector[WormsNode]] =
        val map = scala.collection.mutable.Map.empty[String, Vector[WormsNode]]
        def insert(name: String, node: WormsNode): Unit =
            map.updateWith(name)(existing => Some(existing.getOrElse(Vector.empty) :+ node))
        def add(node: WormsNode): Unit =
            insert(node.name, node)
            node.alternateNames.foreach(n => insert(n, node))
            node.children.foreach(add)
        add(rootNode)
        SortedMap.from(map)

    /**
     * All names used in WoRMS.
     */
    lazy val names: Set[String] = namesMap.keySet

    /**
     * SortedMap of lowercased name → collection of original names. Built once at startup; used to
     * avoid per-element toLowerCase allocations on every search query. A `Vector` of originals is
     * stored per lowercase key to handle names that differ only by case (or Unicode case-folding
     * collisions) without silently dropping entries.
     */
    lazy val lowerNamesMap: SortedMap[String, Vector[String]] =
        val builder = SortedMap.newBuilder[String, Vector[String]]
        val grouped = namesMap.keys.groupBy(_.toLowerCase).map((k, vs) => k -> vs.toVector)
        builder ++= grouped
        builder.result()

    /**
     * Map of [aphiaId, WormsConcept] for O(1) concept lookup by id.
     */
    lazy val wormsConceptsMap: Map[Long, WormsConcept] =
        wormsConcepts.map(c => c.id -> c).toMap

    /**
     * Map of [aphiaId, Node]. Each node appears exactly once, keyed by its own aphiaId.
     */
    lazy val aphiaIdMap: Map[Long, WormsNode] =
        val map                        = Map.newBuilder[Long, WormsNode]
        def add(node: WormsNode): Unit =
            map += node.aphiaId -> node
            node.children.foreach(add)
        add(rootNode)
        map.result()

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

    /**
     * Return all nodes associated with a name (primary or alternate).
     * Returns an empty Seq when the name is not present in the tree.
     */
    def findNodesByName(name: String): Seq[WormsNode] =
        namesMap.getOrElse(name, Vector.empty)

    /**
     * Return the single best node for a name. When multiple nodes share the same name,
     * preference order is: (1) node whose primary name matches exactly, (2) any accepted
     * node, (3) first in insertion order.
     */
    def findNodeByName(name: String): Option[WormsNode] =
        namesMap.get(name).flatMap: nodes =>
            nodes.find(_.name == name)
                .orElse(nodes.find(_.isAccepted))
                .orElse(nodes.headOption)

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
