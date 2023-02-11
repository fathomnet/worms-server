package org.fathomnet.worms.io.extended

import org.fathomnet.worms.WormsNode

object CombineTrees:
  
/**
  * Combine a sequence of trees into a single tree. It adjust the Aphia IDs of the
  * combined tree so that they are unique. The inteneded usage is that the
  * rootNode have a low aphiaId (0), then add the WormS tree first.
  * Then the worms aphiaId's will not be changed. Ony the subsequent trees will
  * have their aphiaId's incremented.
  *
  * @param rootNode The root node of the tree to add the other trees to
  * @param trees The trees to add
  * @return The combined tree
  */
  def combine(rootNode: WormsNode,  trees: Seq[WormsNode]): WormsNode = 
    var root = rootNode
    trees.foreach(t => {
      root = add(root, t, root.maxAphiaId)
    })
    root

  def add(rootNode: WormsNode, tree: WormsNode, maxAphiaId: Long): WormsNode = 
    val node = incrementAphiaId(tree, maxAphiaId)
    val children = rootNode.children :+ node
    rootNode.copy(children = children)
    

  def incrementAphiaId(node: WormsNode, maxAphiaId: Long): WormsNode =
    node.copy(
      aphiaId = node.aphiaId + maxAphiaId,
      children = node.children.map(incrementAphiaId(_, maxAphiaId))
    )

  

