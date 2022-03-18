/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.etc.xml

import org.w3c.dom.NodeList
import org.w3c.dom.Node

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
given Conversion[NodeList, List[Node]] with
  def apply(nodeList: NodeList): List[Node] =
    (0 until nodeList.getLength).map(nodeList.item).toList
