/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io.extended

import org.fathomnet.worms.WormsNode

object CombineTrees:

    /**
     * Combine a sequence of trees into a single tree. It adjust the Aphia IDs of the combined tree so that they are
     * unique. The inteneded usage is that the rootNode have a low aphiaId (0), then add the WormS tree first. Then the
     * worms aphiaId's will not be changed. Ony the subsequent trees will have their aphiaId's incremented and then
     * multiplied by -1 so that all fake aphiaId's are negative.
     *
     * @param rootNode
     *   The root node of the tree to add the other trees to
     * @param trees
     *   The trees to add
     * @param maxAphiaId
     *   The maximum aphiaId of the original WoRMS tree. Only aphiaId's greater than this value will be flipped to
     *   negative so that they are not confused with real aphiaId's.
     * @return
     *   The combined tree
     */
    def combine(rootNode: WormsNode, trees: Seq[WormsNode], maxAphiaId: Long): WormsNode =
        var root = rootNode
        trees.foreach(t => root = add(root, t, root.maxAphiaId))
        flipFakeAphiaId(root, maxAphiaId)

    def add(rootNode: WormsNode, tree: WormsNode, maxAphiaId: Long): WormsNode =
        val node     = incrementAphiaId(tree, maxAphiaId)
        val children = rootNode.children :+ node
        rootNode.copy(children = children)

    def incrementAphiaId(node: WormsNode, maxAphiaId: Long): WormsNode =
        val newAphiaId = node.aphiaId + maxAphiaId
        node.copy(
            aphiaId = newAphiaId,
            children = node.children.map(incrementAphiaId(_, maxAphiaId))
        )

    /**
     * Flip the aphiaId of all nodes that have aphiaId > maxAphiaId so that fake aphiaId's are negative.
     *
     * @param node
     *   The node to flip
     * @param maxAphiaId
     *   The maximum aphiaId of the original tree
     * @return
     *   A new tree with fake aphiaId's flipped to negative
     */
    def flipFakeAphiaId(node: WormsNode, maxAphiaId: Long): WormsNode =
        val newAphiaId         = if (node.aphiaId > maxAphiaId) -node.aphiaId else node.aphiaId
        val newAcceptedAphiaId = if (newAphiaId < 0) newAphiaId else node.acceptedAphiaId
        node.copy(
            aphiaId = newAphiaId,
            acceptedAphiaId = newAcceptedAphiaId,
            children = node.children.map(n => flipFakeAphiaId(n, maxAphiaId))
        )
