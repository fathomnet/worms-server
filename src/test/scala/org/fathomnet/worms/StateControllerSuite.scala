/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import org.fathomnet.worms.io.{WormsConcept, WormsConceptName}

class StateControllerSuite extends munit.FunSuite:

    // ---- test tree --------------------------------------------------------
    //   Animalia (2, alternateNames=["animals"])
    //     Mollusca (51)
    //       Polyplacophora (55, alternateNames=["chitons", "shells"])  ← "shells" shared
    //     Arthropoda (100)
    //       Decapoda (120, alternateNames=["decapods", "shells"])      ← "shells" shared
    //     OldArthropoda (200, acceptedAphiaId=100)  ← synonym for Arthropoda
    // -----------------------------------------------------------------------
    // "shells" is intentionally shared between Polyplacophora and Decapoda to verify
    // that the namesMap multimap correctly preserves both nodes and that descendant
    // lookups by a shared alternate name return the union of their descendants.

    private val polyplacophora = WormsNode("Polyplacophora", "Class",   55,  55,  Seq("chitons", "shells"),  Nil)
    private val mollusca       = WormsNode("Mollusca",       "Phylum",  51,  51,  Nil,                       Seq(polyplacophora))
    private val decapoda       = WormsNode("Decapoda",       "Order",   120, 120, Seq("decapods", "shells"),  Nil)
    private val arthropoda     = WormsNode("Arthropoda",     "Phylum",  100, 100, Nil,                       Seq(decapoda))
    private val oldArthropoda  = WormsNode("OldArthropoda",  "Phylum",  200, 100, Nil,                       Nil)
    private val root           = WormsNode("Animalia",       "Kingdom", 2,   2,   Seq("animals"),            Seq(mollusca, arthropoda, oldArthropoda))

    private def cn(name: String, primary: Boolean = true) = WormsConceptName(name, primary)

    private val wormsConcepts = Seq(
        WormsConcept(2,   2,   None,      Seq(cn("Animalia"), cn("animals", false)),                               "Kingdom", isMarine = Some(true)),
        WormsConcept(51,  51,  Some(2),   Seq(cn("Mollusca")),                                                     "Phylum",  isMarine = Some(true)),
        WormsConcept(55,  55,  Some(51),  Seq(cn("Polyplacophora"), cn("chitons", false), cn("shells", false)),    "Class",   isMarine = Some(true)),
        WormsConcept(100, 100, Some(2),   Seq(cn("Arthropoda")),                                                   "Phylum"),
        WormsConcept(120, 120, Some(100), Seq(cn("Decapoda"), cn("decapods", false), cn("shells", false)),         "Order"),
        WormsConcept(200, 100, Some(2),   Seq(cn("OldArthropoda")),                                               "Phylum"),
    )

    private val testData = Data(root, wormsConcepts)

    override def beforeEach(context: BeforeEach): Unit =
        State.data = Some(testData)

    override def afterAll(): Unit =
        State.data = None

    // ---- absent state -----------------------------------------------------

    test("runSearch returns NotFound when State.data is absent"):
        State.data = None
        val result = StateController.findAllNames(10, 0)
        assert(result.isLeft)

    // ---- findAllNames / countAllNames -------------------------------------

    test("findAllNames returns paginated results with correct total"):
        // 10 distinct names: Animalia, animals, Arthropoda, chitons, Decapoda,
        // decapods, Mollusca, OldArthropoda, Polyplacophora, shells
        // ("shells" is shared between Polyplacophora and Decapoda but counts once)
        val page = StateController.findAllNames(3, 0).getOrElse(fail("expected Right"))
        assertEquals(page.limit, 3)
        assertEquals(page.offset, 0)
        assertEquals(page.total, 10)
        assertEquals(page.items.size, 3)

    test("findAllNames honours offset"):
        val page = StateController.findAllNames(3, 6).getOrElse(fail("expected Right"))
        assertEquals(page.offset, 6)
        assertEquals(page.items.size, 3)

    test("countAllNames returns total number of distinct names"):
        assertEquals(StateController.countAllNames().getOrElse(fail("expected Right")), 10)

    // ---- queryNames -------------------------------------------------------

    test("queryNamesStartingWith returns names with matching prefix"):
        assertEquals(StateController.queryNamesStartingWith("Mo").getOrElse(fail("expected Right")), List("Mollusca"))

    test("queryNamesStartingWith is case-insensitive"):
        assertEquals(StateController.queryNamesStartingWith("mo").getOrElse(fail("expected Right")), List("Mollusca"))

    test("queryNamesStartingWith returns empty list for unknown prefix"):
        assert(StateController.queryNamesStartingWith("xyz").getOrElse(fail("expected Right")).isEmpty)

    test("queryNamesContaining returns names containing substring"):
        val names = StateController.queryNamesContaining("rth").getOrElse(fail("expected Right")).sorted
        assert(names.contains("Arthropoda"))
        assert(names.contains("OldArthropoda"))

    // ---- findNamesByAphiaId -----------------------------------------------

    test("findNamesByAphiaId returns Names for a known aphiaId"):
        val names = StateController.findNamesByAphiaId(2).getOrElse(fail("expected Right"))
        assertEquals(names.aphiaId, 2L)
        assertEquals(names.name, "Animalia")
        assertEquals(names.acceptedName, "Animalia")
        assert(names.alternateNames.contains("animals"))

    test("findNamesByAphiaId includes synonyms in alternateNames"):
        val names = StateController.findNamesByAphiaId(100).getOrElse(fail("expected Right"))
        assertEquals(names.acceptedName, "Arthropoda")
        assert(names.alternateNames.contains("OldArthropoda"))

    test("findNamesByAphiaId returns NotFound for unknown aphiaId"):
        assert(StateController.findNamesByAphiaId(9999).isLeft)

    // ---- descendantNames --------------------------------------------------

    test("descendantNames returns all descendant names of a node"):
        val names = StateController.descendantNames("Mollusca").getOrElse(fail("expected Right"))
        assert(names.contains("Mollusca"))
        assert(names.contains("Polyplacophora"))

    test("descendantNames returns NotFound for unknown name"):
        assert(StateController.descendantNames("Unknown").isLeft)

    test("descendantNames via a shared alternate name returns union of descendants from all matching nodes"):
        // "shells" is an alternate name for both Polyplacophora (55) and Decapoda (120).
        // Before the multimap fix, whichever node was inserted last would silently overwrite
        // the other; the "lost" node's descendants would be missing from the result.
        val names = StateController.descendantNames("shells").getOrElse(fail("expected Right"))
        assert(names.contains("Polyplacophora"), s"expected Polyplacophora in $names")
        assert(names.contains("Decapoda"),       s"expected Decapoda in $names")

    test("findNodesByName returns all nodes sharing a name"):
        val nodes = State.data.get.findNodesByName("shells")
        assertEquals(nodes.size, 2)
        assert(nodes.map(_.name).toSet == Set("Polyplacophora", "Decapoda"))

    test("findNodeByName on a shared alternate name prefers the accepted node"):
        val node = State.data.get.findNodeByName("shells").getOrElse(fail("expected Some"))
        assert(node.isAccepted, s"expected accepted node but got ${node.name} (acceptedAphiaId=${node.acceptedAphiaId})")

    // ---- ancestorNames ----------------------------------------------------

    test("ancestorNames returns full path from root to node"):
        val names = StateController.ancestorNames("Polyplacophora").getOrElse(fail("expected Right"))
        assertEquals(names, List("Animalia", "Mollusca", "Polyplacophora"))

    test("ancestorNames for root returns only the root"):
        val names = StateController.ancestorNames("Animalia").getOrElse(fail("expected Right"))
        assertEquals(names, List("Animalia"))

    // ---- childNames -------------------------------------------------------

    test("childNames returns sorted direct children"):
        val names = StateController.childNames("Animalia").getOrElse(fail("expected Right"))
        assertEquals(names, List("Arthropoda", "Mollusca", "OldArthropoda"))

    test("childNames returns NotFound for a leaf node"):
        assert(StateController.childNames("Polyplacophora").isLeft)

    // ---- parentName -------------------------------------------------------

    test("parentName returns the parent of a node"):
        assertEquals(StateController.parentName("Mollusca").getOrElse(fail("expected Right")), "Animalia")

    test("parentName resolves via alternate name"):
        assertEquals(StateController.parentName("chitons").getOrElse(fail("expected Right")), "Mollusca")

    test("parentName returns NotFound for the root node"):
        assert(StateController.parentName("Animalia").isLeft)

    // ---- synonyms ---------------------------------------------------------

    test("synonyms for an accepted name returns it first, followed by synonyms"):
        val names = StateController.synonyms("Arthropoda").getOrElse(fail("expected Right"))
        assertEquals(names.head, "Arthropoda")
        assert(names.contains("OldArthropoda"))

    test("synonyms for an unaccepted name returns the accepted name first"):
        val names = StateController.synonyms("OldArthropoda").getOrElse(fail("expected Right"))
        assertEquals(names.head, "Arthropoda")
        assert(names.contains("OldArthropoda"))

    // ---- descendantTaxa / ancestorTaxa ------------------------------------

    test("descendantTaxa returns the subtree rooted at the named node"):
        val node = StateController.descendantTaxa("Mollusca").getOrElse(fail("expected Right"))
        assertEquals(node.name, "Mollusca")
        assertEquals(node.children.map(_.name), Seq("Polyplacophora"))

    test("descendantTaxa returns NotFound for unknown name"):
        assert(StateController.descendantTaxa("Unknown").isLeft)

    test("ancestorTaxa returns a linear path from root down to the named node"):
        val node = StateController.ancestorTaxa("Decapoda").getOrElse(fail("expected Right"))
        assertEquals(node.name, "Animalia")
        assertEquals(node.children.size, 1)
        assertEquals(node.children.head.name, "Arthropoda")
        assertEquals(node.children.head.children.head.name, "Decapoda")

    // ---- taxaInfo / parentTaxa / childTaxa --------------------------------

    test("taxaInfo returns a SimpleWormsNode for a known name"):
        val node = StateController.taxaInfo("Animalia").getOrElse(fail("expected Right"))
        assertEquals(node.name, "Animalia")
        assertEquals(node.rank, "Kingdom")
        assertEquals(node.aphiaId, 2L)
        assertEquals(node.acceptedAphiaId, 2L)

    test("taxaInfo returns NotFound for unknown name"):
        assert(StateController.taxaInfo("Unknown").isLeft)

    test("parentTaxa returns the parent as a SimpleWormsNode"):
        val node = StateController.parentTaxa("Decapoda").getOrElse(fail("expected Right"))
        assertEquals(node.name, "Arthropoda")

    test("childTaxa returns sorted children as SimpleWormsNodes"):
        val children = StateController.childTaxa("Animalia").getOrElse(fail("expected Right"))
        assertEquals(children.map(_.name), List("Arthropoda", "Mollusca", "OldArthropoda"))

    // ---- taxaByNameStartingWith -------------------------------------------

    test("taxaByNameStartingWith returns matching taxa"):
        val nodes = StateController.taxaByNameStartingWith("Mo").getOrElse(fail("expected Right"))
        assertEquals(nodes.map(_.name), List("Mollusca"))

    test("taxaByNameStartingWith filters results by rank"):
        val byPhylum = StateController.taxaByNameStartingWith("Mo", rankOpt = Some("Phylum")).getOrElse(fail("expected Right"))
        assertEquals(byPhylum.map(_.name), List("Mollusca"))

        val byClass = StateController.taxaByNameStartingWith("Mo", rankOpt = Some("Class")).getOrElse(fail("expected Right"))
        assert(byClass.isEmpty)

    test("taxaByNameStartingWith scopes results to descendants of a parent"):
        val nodes = StateController.taxaByNameStartingWith("dec", parentOpt = Some("Arthropoda")).getOrElse(fail("expected Right"))
        assert(nodes.map(_.name).contains("Decapoda"))

    // ---- details ----------------------------------------------------------

    test("details returns full WoRMS details for a known name"):
        val d = StateController.details("Animalia").getOrElse(fail("expected Right"))
        assertEquals(d.name, "Animalia")
        assertEquals(d.rank, "Kingdom")
        assertEquals(d.aphiaId, 2L)
        assertEquals(d.isMarine, Some(true))
        assert(d.alternateNames.contains("animals"))

    test("details returns NotFound for unknown name"):
        assert(StateController.details("Unknown").isLeft)
