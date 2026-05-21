/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import io.circe.Decoder
import org.fathomnet.worms.etc.circe.CirceCodecs.given
import org.fathomnet.worms.{Data, SimpleWormsNode, State, WormsNode}
import org.fathomnet.worms.io.WormsLoader
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.Uri
import sttp.tapir.server.stub.TapirStubInterpreter

import java.nio.file.Paths
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

class TaxaEndpointsSuite extends munit.FunSuite:

    given ExecutionContext = ExecutionContext.global

    // Load the faketree once. After the pipeline the tree is rooted at "Biota"
    // with "Animalia" as its only child, then "Mollusca" (Phylum) → "Polyplacophora" (Class), etc.
    override def beforeAll(): Unit =
        val fakeTreePath             = Paths.get(getClass.getResource("/faketree").toURI)
        val (wormsConcepts, rootOpt) = WormsLoader.load(fakeTreePath)
        State.data = rootOpt.map(root => Data(root, wormsConcepts))

    override def afterAll(): Unit =
        State.data = None

    private lazy val endpoints = TaxaEndpoints()

    private lazy val backend =
        TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
            .whenServerEndpointsRunLogic(endpoints.all)
            .backend()

    private def getJson[T: Decoder](uri: Uri): T =
        Await.result(basicRequest.get(uri).response(asJson[T]).send(backend), 5.seconds)
            .body
            .getOrElse(fail(s"GET $uri returned an error body"))

    private def statusCode(uri: Uri): Int =
        Await.result(basicRequest.get(uri).send(backend), 5.seconds).code.code

    // ---- GET /taxa/descendants/{name} ------------------------------------

    test("GET /taxa/descendants/Mollusca returns a WormsNode subtree"):
        val node = getJson[WormsNode](uri"http://localhost/taxa/descendants/Mollusca")
        assertEquals(node.name, "Mollusca")
        assertEquals(node.aphiaId, 51L)
        assert(node.children.nonEmpty, "Expected Mollusca to have children (e.g. Polyplacophora)")

    test("GET /taxa/descendants includes recursive descendants"):
        val node = getJson[WormsNode](uri"http://localhost/taxa/descendants/Mollusca")
        assert(node.descendantNames.contains("Polyplacophora"), s"Expected Polyplacophora in descendants of Mollusca")

    test("GET /taxa/descendants returns the accepted name and rank"):
        val node = getJson[WormsNode](uri"http://localhost/taxa/descendants/Animalia")
        assertEquals(node.name, "Animalia")
        assertEquals(node.rank, "Kingdom")

    test("GET /taxa/descendants for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/taxa/descendants/UnknownTaxon"), 404)

    // ---- GET /taxa/ancestors/{name} --------------------------------------

    test("GET /taxa/ancestors/Mollusca returns a linear path from root to Mollusca"):
        val node = getJson[WormsNode](uri"http://localhost/taxa/ancestors/Mollusca")
        assertEquals(node.name, "Biota")
        assertEquals(node.children.size, 1)
        assertEquals(node.children.head.name, "Animalia")
        assertEquals(node.children.head.children.head.name, "Mollusca")

    test("GET /taxa/ancestors leaf node has its children trimmed"):
        val node = getJson[WormsNode](uri"http://localhost/taxa/ancestors/Mollusca")
        assert(node.children.head.children.head.children.isEmpty, "Ancestor leaf should have no children")

    test("GET /taxa/ancestors for root returns just the root"):
        val node = getJson[WormsNode](uri"http://localhost/taxa/ancestors/Biota")
        assertEquals(node.name, "Biota")
        assert(node.children.isEmpty, "Root ancestor path should have no children")

    test("GET /taxa/ancestors for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/taxa/ancestors/UnknownTaxon"), 404)

    // ---- GET /taxa/info/{name} -------------------------------------------

    test("GET /taxa/info/Animalia returns a SimpleWormsNode with correct fields"):
        val node = getJson[SimpleWormsNode](uri"http://localhost/taxa/info/Animalia")
        assertEquals(node.name, "Animalia")
        assertEquals(node.rank, "Kingdom")
        assertEquals(node.aphiaId, 2L)
        assertEquals(node.acceptedAphiaId, 2L)

    test("GET /taxa/info includes vernacular names as alternateNames"):
        val node = getJson[SimpleWormsNode](uri"http://localhost/taxa/info/Animalia")
        assert(node.alternateNames.nonEmpty, "Expected vernacular names in alternateNames")

    test("GET /taxa/info resolves by alternate name"):
        // "animals" is a vernacular name for Animalia; looking it up should resolve to the same node
        val node = getJson[SimpleWormsNode](uri"http://localhost/taxa/info/animals")
        assertEquals(node.name, "Animalia")

    test("GET /taxa/info for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/taxa/info/UnknownTaxon"), 404)

    // ---- GET /taxa/parent/{name} -----------------------------------------

    test("GET /taxa/parent/Mollusca returns a SimpleWormsNode for Animalia"):
        val node = getJson[SimpleWormsNode](uri"http://localhost/taxa/parent/Mollusca")
        assertEquals(node.name, "Animalia")

    test("GET /taxa/parent resolves parent via alternate name"):
        // "chitons" is an alternate name for Polyplacophora; its parent should be Mollusca
        val node = getJson[SimpleWormsNode](uri"http://localhost/taxa/parent/chitons")
        assertEquals(node.name, "Mollusca")

    test("GET /taxa/parent for root returns 404"):
        assertEquals(statusCode(uri"http://localhost/taxa/parent/Biota"), 404)

    test("GET /taxa/parent for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/taxa/parent/UnknownTaxon"), 404)

    // ---- GET /taxa/children/{name} ---------------------------------------

    test("GET /taxa/children/Animalia returns a sorted list of SimpleWormsNodes"):
        val children = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/children/Animalia")
        val names    = children.map(_.name)
        assert(names.contains("Mollusca"), s"Expected Mollusca in children of Animalia: $names")
        assertEquals(names, names.sorted, "Children should be sorted alphabetically")

    test("GET /taxa/children/Mollusca returns Polyplacophora"):
        val children = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/children/Mollusca")
        assert(children.map(_.name).contains("Polyplacophora"), s"Expected Polyplacophora in children of Mollusca")

    test("GET /taxa/children for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/taxa/children/UnknownTaxon"), 404)

    // ---- GET /taxa/query/startswith/{prefix} -----------------------------

    test("GET /taxa/query/startswith/Mol returns SimpleWormsNodes starting with 'Mol'"):
        val nodes = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/Mol")
        val names = nodes.map(_.name)
        assert(names.contains("Mollusca"), s"Expected Mollusca in $names")
        assert(names.forall(_.toLowerCase.startsWith("mol")), "All names must start with 'mol'")

    test("GET /taxa/query/startswith is case-insensitive"):
        val lower = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/mol").map(_.name)
        val upper = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/Mol").map(_.name)
        assertEquals(lower.sorted, upper.sorted)

    test("GET /taxa/query/startswith with unknown prefix returns empty list"):
        val nodes = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/zzzzz")
        assert(nodes.isEmpty, s"Expected empty list, got $nodes")

    test("GET /taxa/query/startswith filters by rank"):
        val byPhylum = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/Mol?rank=Phylum")
        assert(byPhylum.map(_.name).contains("Mollusca"), "Expected Mollusca with rank=Phylum")

        val byClass = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/Mol?rank=Class")
        assert(!byClass.map(_.name).contains("Mollusca"), "Mollusca is Phylum, not Class")

    test("GET /taxa/query/startswith scopes results to descendants of a parent"):
        val nodes = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/Po?parent=Mollusca")
        val names = nodes.map(_.name)
        assert(names.contains("Polyplacophora"), s"Expected Polyplacophora in $names")

    test("GET /taxa/query/startswith with unknown parent returns empty list"):
        val nodes = getJson[List[SimpleWormsNode]](uri"http://localhost/taxa/query/startswith/Mol?parent=UnknownTaxon")
        assert(nodes.isEmpty, s"Expected empty list for unknown parent, got $nodes")
