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
    aphiaId: Long,
    acceptedAphiaId: Long,
    alternateNames: Seq[String],
    children: Seq[WormsNode]
):

    lazy val isAccepted: Boolean = this.aphiaId == this.acceptedAphiaId

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

    lazy val descendants: Seq[WormsNode] =
        this.children.foldLeft(Seq(this): Seq[WormsNode]) { (acc, child) =>
            acc ++ child.descendants
        }

    // lazy val descendantNamesWithVernacular: Seq[String] =
    //   this.children.foldLeft(this.names: Seq[String]) { (acc, child) =>
    //     acc ++ child.descendantNamesWithVernacular
    //   }

    lazy val names: Seq[String] =
        (this.name +: this.alternateNames.sorted).toSeq

    /**
     * A version of this node with no children
     */
    lazy val simple: SimpleWormsNode = SimpleWormsNode(
        name = this.name,
        rank = this.rank,
        aphiaId = this.aphiaId,
        acceptedAphiaId = this.acceptedAphiaId,
        alternateNames = this.alternateNames
    )

    /**
     * The maximum Aphia ID of this node and all its descendants
     */
    lazy val maxAphiaId: Long =
        this.children.foldLeft(this.aphiaId) { (acc, child) =>
            acc.max(child.maxAphiaId)
        }

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
        WormsNode(
            name,
            node.concept.rank,
            node.concept.id,
            node.concept.acceptedId,
            alternateNames,
            node.children.map(from).toSeq
        )

final case class SimpleWormsNode(
    name: String,
    rank: String,
    aphiaId: Long,
    acceptedAphiaId: Long,
    alternateNames: Seq[String]
)
