/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io

import java.nio.file.Paths
import scala.concurrent.ExecutionContext

class WormsLoaderSuite extends munit.FunSuite:

    given ExecutionContext = ExecutionContext.global

    private def fakeTreePath = Paths.get(getClass.getResource("/faketree").toURI)

    test("load returns non-empty concepts and a root node"):
        val (concepts, root) = WormsLoader.load(fakeTreePath)
        assert(concepts.nonEmpty, "Expected non-empty concepts")
        assert(root.isDefined, "Expected a root node")

    test("load parses all taxon rows as concepts"):
        val (concepts, _) = WormsLoader.load(fakeTreePath)
        assert(concepts.size >= 100, s"Expected >= 100 concepts, got ${concepts.size}")

    test("root node is Biota"):
        val (_, root) = WormsLoader.load(fakeTreePath)
        assert(root.isDefined)
        assertEquals(root.get.name, "Biota")

    test("Animalia is reachable in the tree"):
        val (_, root) = WormsLoader.load(fakeTreePath)
        assert(root.isDefined)
        val animalia = root.get.find("Animalia")
        assert(animalia.isDefined, "Expected to find Animalia in the tree")

    test("vernacular names appear as alternate names"):
        // Animalia (taxon ID 2) has vernacular names: "animals", "dieren", "animaux", etc.
        val (_, root) = WormsLoader.load(fakeTreePath)
        assert(root.isDefined)
        val animalia = root.get.find("Animalia")
        assert(animalia.isDefined, "Expected to find Animalia")
        assert(
            animalia.get.alternateNames.nonEmpty,
            s"Expected Animalia to have vernacular alternate names, got none"
        )

    test("species profile isMarine is populated"):
        val (concepts, _) = WormsLoader.load(fakeTreePath)
        val marineConcepts = concepts.filter(_.isMarine.contains(true))
        assert(marineConcepts.nonEmpty, "Expected at least one concept with isMarine = true")

    test("root node has descendants"):
        val (_, root) = WormsLoader.load(fakeTreePath)
        assert(root.isDefined)
        assert(root.get.children.nonEmpty, "Expected root to have children")
        assert(root.get.descendantNames.size > 1, "Expected multiple descendants under root")
