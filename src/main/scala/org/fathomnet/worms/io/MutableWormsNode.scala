/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import scala.collection.mutable

case class MutableWormsNode(
    concept: WormsConcept,
    children: mutable.ArrayBuffer[MutableWormsNode] = mutable.ArrayBuffer.empty
)

/**
 * Organizes the flat wormsconceps read from the source files into a tree structure.
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
object MutableWormsNodeBuilder:

    def fathomNetTree(wormsConcepts: Seq[WormsConcept]): MutableWormsNode =
        trimTree(buildTree(filterFlattenedTree(wormsConcepts)))

    /**
     * Given a list of WormsConcepts, organize them into a tree structure. and return the root node. THis is the FULL
     * worms tree
     * @param wormsConcepts
     *   WormsConcepts loaded from [[WormsLoader]]
     * @return
     *   The root node of the tree
     */
    def buildTree(wormsConcepts: Seq[WormsConcept]): MutableWormsNode =
        val map = mutable.Map[Long, MutableWormsNode]()
        wormsConcepts.foreach(c => map.put(c.id, MutableWormsNode(c)))
        for
            c           <- wormsConcepts
            conceptNode <- map.get(c.id)
            parentId    <- c.parentId
            parentNode  <- map.get(parentId)
        do parentNode.children.append(conceptNode)
        // In worms, the root node has an aphiaId of 1
        val minAphiaId = wormsConcepts.map(_.id).min
        map.get(minAphiaId).get

    /**
     * Given the full tree, return only the animalia node
     * @param rootNode
     *   the root node of the tree from [[buildTree]]
     * @return
     *   the animalia node
     */
    def trimTree(rootNode: MutableWormsNode): MutableWormsNode =
        def filter(children: Seq[MutableWormsNode]): Seq[MutableWormsNode] =
            children.filter(_.concept.names.map(_.name).exists(_.contains("Animalia")))

        val accepted = filter(rootNode.children.toSeq)
        if (accepted.isEmpty) rootNode
        else MutableWormsNode(rootNode.concept, mutable.ArrayBuffer().appendAll(accepted))

    /**
     * Prune off extinct species from the list of WormsConcepts
     * @param rows
     *   the list of WormsConcepts
     * @return
     *   the list of WormsConcepts with extinct species removed
     */
    def filterFlattenedTree(rows: Seq[WormsConcept]): Seq[WormsConcept] =
        rows.filter(wc =>
            val lr = wc.rank.toLowerCase
            // (!lr.contains("species") && !lr.contains("variety")) || (!wc.isExtinct && wc.isMarine)
            (!lr.contains("species") && !lr.contains("variety")) || (!wc.isExtinct)
        )

    /**
     * Squash the tree into a flat list of WormsConcepts. This is useful if you need to convert a tree into rows to load
     * into a database.
     * @param rootNode
     *   the root node of the tree or branch
     * @return
     *   the list of WormsConcepts
     */
    def flattenTree(node: MutableWormsNode): Seq[WormsConcept] =
        node.concept +: node.children.toSeq.flatMap(flattenTree)

    /**
     * Given a node, print out an indented tree structure
     * @param node
     *   the root node of the tree or branch
     */
    def printTree(node: MutableWormsNode, indent: Int = 0): Unit =
        println(s"${"  " * indent}${node.concept.names.map(_.name).mkString(", ")}")
        node.children.foreach(printTree(_, indent + 1))
