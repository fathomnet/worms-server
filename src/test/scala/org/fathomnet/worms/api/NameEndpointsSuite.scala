/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import io.circe.Decoder
import io.circe.generic.auto.*
import org.fathomnet.worms.{Data, Names, Page, State}
import org.fathomnet.worms.io.WormsLoader
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.Uri
import sttp.tapir.server.stub.TapirStubInterpreter

import java.nio.file.Paths
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

class NameEndpointsSuite extends munit.FunSuite:

    given ExecutionContext = ExecutionContext.global

    // Load the faketree once for the entire suite and populate State.data.
    // The pipeline (WormsLoader → trimTree → Animalia subtree) produces a tree
    // rooted at "Biota" with "Animalia" as its only child.
    override def beforeAll(): Unit =
        val fakeTreePath             = Paths.get(getClass.getResource("/faketree").toURI)
        val (wormsConcepts, rootOpt) = WormsLoader.load(fakeTreePath)
        State.data = rootOpt.map(root => Data(root, wormsConcepts))

    override def afterAll(): Unit =
        State.data = None

    private lazy val endpoints = NameEndpoints()

    private lazy val backend =
        TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
            .whenServerEndpointsRunLogic(endpoints.all)
            .backend()

    // Helpers to keep test bodies concise.
    private def getJson[T: Decoder](uri: Uri): T =
        Await.result(basicRequest.get(uri).response(asJson[T]).send(backend), 5.seconds)
            .body
            .getOrElse(fail(s"GET $uri returned an error body"))

    private def statusCode(uri: Uri): Int =
        Await.result(basicRequest.get(uri).send(backend), 5.seconds).code.code

    // ---- GET /names -------------------------------------------------------

    test("GET /names returns a Page with the requested limit and a positive total"):
        val page = getJson[Page[String]](uri"http://localhost/names?limit=10&offset=0")
        assertEquals(page.limit, 10)
        assertEquals(page.offset, 0)
        assert(page.total > 0, s"Expected total > 0, got ${page.total}")
        assertEquals(page.items.size, 10)

    test("GET /names respects offset"):
        val first  = getJson[Page[String]](uri"http://localhost/names?limit=5&offset=0")
        val second = getJson[Page[String]](uri"http://localhost/names?limit=5&offset=5")
        assertEquals(second.offset, 5)
        assertEquals(second.items.size, 5)
        assert(first.items.toSet.intersect(second.items.toSet).isEmpty, "Pages must not overlap")

    test("GET /names uses default limit and offset when params are absent"):
        val page = getJson[Page[String]](uri"http://localhost/names")
        assertEquals(page.limit, 100)
        assertEquals(page.offset, 0)

    // ---- GET /names/count ------------------------------------------------

    test("GET /names/count returns a positive integer"):
        val count = getJson[Int](uri"http://localhost/names/count")
        assert(count > 0, s"Expected count > 0, got $count")

    test("GET /names/count matches the total field in /names"):
        val count = getJson[Int](uri"http://localhost/names/count")
        val page  = getJson[Page[String]](uri"http://localhost/names?limit=1&offset=0")
        assertEquals(count, page.total)

    // ---- GET /names/aphiaid/{id} -----------------------------------------

    test("GET /names/aphiaid/2 returns Names for Animalia"):
        val names = getJson[Names](uri"http://localhost/names/aphiaid/2")
        assertEquals(names.aphiaId, 2L)
        assertEquals(names.name, "Animalia")
        assertEquals(names.acceptedName, "Animalia")
        // Animalia has vernacular names loaded from vernacularname.txt
        assert(names.alternateNames.nonEmpty, "Expected vernacular names in alternateNames")

    test("GET /names/aphiaid/999999 returns 404"):
        assertEquals(statusCode(uri"http://localhost/names/aphiaid/999999"), 404)

    // ---- GET /query/startswith/{prefix} ----------------------------------

    test("GET /query/startswith/Mol returns names starting with 'Mol'"):
        val names = getJson[List[String]](uri"http://localhost/query/startswith/Mol")
        assert(names.contains("Mollusca"), s"Expected Mollusca in $names")
        assert(names.forall(_.toLowerCase.startsWith("mol")), "All names must start with 'mol'")

    test("GET /query/startswith prefix search is case-insensitive"):
        val lower = getJson[List[String]](uri"http://localhost/query/startswith/mol")
        val upper = getJson[List[String]](uri"http://localhost/query/startswith/Mol")
        assertEquals(lower.sorted, upper.sorted)

    test("GET /query/startswith with unknown prefix returns empty list"):
        val names = getJson[List[String]](uri"http://localhost/query/startswith/zzzzzz")
        assert(names.isEmpty)

    // ---- GET /query/contains/{glob} --------------------------------------

    test("GET /query/contains/lusca returns names containing 'lusca'"):
        val names = getJson[List[String]](uri"http://localhost/query/contains/lusca")
        assert(names.contains("Mollusca"), s"Expected Mollusca in $names")

    test("GET /query/contains with unknown substring returns empty list"):
        val names = getJson[List[String]](uri"http://localhost/query/contains/zzzzzz")
        assert(names.isEmpty)

    // ---- GET /descendants/{name} -----------------------------------------

    test("GET /descendants/Mollusca returns Mollusca and its descendants"):
        val names = getJson[List[String]](uri"http://localhost/descendants/Mollusca")
        assert(names.contains("Mollusca"), s"Expected Mollusca in $names")
        assert(names.contains("Polyplacophora"), s"Expected Polyplacophora in $names")

    test("GET /descendants with accepted=true filters to accepted names only"):
        val all      = getJson[List[String]](uri"http://localhost/descendants/Animalia")
        val accepted = getJson[List[String]](uri"http://localhost/descendants/Animalia?accepted=true")
        assert(accepted.size <= all.size, s"accepted(${accepted.size}) should be <= all(${all.size})")

    test("GET /descendants for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/descendants/UnknownTaxon"), 404)

    // ---- GET /ancestors/{name} -------------------------------------------

    test("GET /ancestors/Mollusca returns path from root to Mollusca"):
        val path = getJson[List[String]](uri"http://localhost/ancestors/Mollusca")
        assert(path.contains("Animalia"), s"Expected Animalia in ancestor path $path")
        assert(path.contains("Mollusca"), s"Expected Mollusca in ancestor path $path")
        assertEquals(path.last, "Mollusca")

    test("GET /ancestors for root returns only the root"):
        val path = getJson[List[String]](uri"http://localhost/ancestors/Biota")
        assertEquals(path, List("Biota"))

    // ---- GET /children/{name} --------------------------------------------

    test("GET /children/Animalia returns its direct children"):
        val children = getJson[List[String]](uri"http://localhost/children/Animalia")
        assert(children.contains("Mollusca"), s"Expected Mollusca in $children")
        assertEquals(children, children.sorted, "Children should be sorted alphabetically")

    test("GET /children for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/children/UnknownTaxon"), 404)

    // ---- GET /parent/{name} ---------------------------------------------

    test("GET /parent/Mollusca returns Animalia"):
        val parent = getJson[String](uri"http://localhost/parent/Mollusca")
        assertEquals(parent, "Animalia")

    test("GET /parent for root returns 404"):
        assertEquals(statusCode(uri"http://localhost/parent/Biota"), 404)

    // ---- GET /synonyms/{name} -------------------------------------------

    test("GET /synonyms/Animalia returns Animalia as the first (accepted) name"):
        val names = getJson[List[String]](uri"http://localhost/synonyms/Animalia")
        assert(names.nonEmpty, "Expected non-empty synonym list")
        assertEquals(names.head, "Animalia")
        // Vernacular names from vernacularname.txt (e.g. "animals") appear as synonyms
        assert(names.size > 1, "Expected vernacular names in synonym list")
