/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.api

import io.circe.Decoder
import org.fathomnet.worms.etc.circe.CirceCodecs.given
import org.fathomnet.worms.{Data, State, WormsDetails}
import org.fathomnet.worms.io.WormsLoader
import sttp.client3.*
import sttp.client3.circe.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.Uri
import sttp.tapir.server.stub.TapirStubInterpreter

import java.nio.file.Paths
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

class DetailEndpointsSuite extends munit.FunSuite:

    given ExecutionContext = ExecutionContext.global

    override def beforeAll(): Unit =
        val fakeTreePath             = Paths.get(getClass.getResource("/faketree").toURI)
        val (wormsConcepts, rootOpt) = WormsLoader.load(fakeTreePath)
        State.data = rootOpt.map(root => Data(root, wormsConcepts))

    override def afterAll(): Unit =
        State.data = None

    private lazy val endpoints = DetailEndpoints()

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

    // ---- GET /details/{name} ---------------------------------------------

    test("GET /details/Animalia returns correct name, rank, and aphiaId"):
        val d = getJson[WormsDetails](uri"http://localhost/details/Animalia")
        assertEquals(d.name, "Animalia")
        assertEquals(d.rank, "Kingdom")
        assertEquals(d.aphiaId, 2L)

    test("GET /details/Animalia has correct parentAphiaId pointing to Biota"):
        val d = getJson[WormsDetails](uri"http://localhost/details/Animalia")
        assertEquals(d.parentAphiaId, Some(1L))

    test("GET /details/Animalia includes vernacular names as alternateNames"):
        val d = getJson[WormsDetails](uri"http://localhost/details/Animalia")
        assert(d.alternateNames.nonEmpty, "Expected vernacular names in alternateNames")

    test("GET /details/Animalia reflects species-profile flags"):
        val d = getJson[WormsDetails](uri"http://localhost/details/Animalia")
        assertEquals(d.isMarine, Some(true))
        assertEquals(d.isFreshwater, Some(true))
        assertEquals(d.isTerrestrial, Some(true))

    test("GET /details/Mollusca returns correct name, rank, and parent"):
        val d = getJson[WormsDetails](uri"http://localhost/details/Mollusca")
        assertEquals(d.name, "Mollusca")
        assertEquals(d.rank, "Phylum")
        assertEquals(d.aphiaId, 51L)
        assertEquals(d.parentAphiaId, Some(2L))
        assertEquals(d.isMarine, Some(true))

    test("GET /details/Biota returns root with no parentAphiaId"):
        val d = getJson[WormsDetails](uri"http://localhost/details/Biota")
        assertEquals(d.name, "Biota")
        assertEquals(d.parentAphiaId, None)

    test("GET /details resolves an alternate name to its accepted concept"):
        // "animals" is a vernacular name for Animalia
        val d = getJson[WormsDetails](uri"http://localhost/details/animals")
        assertEquals(d.name, "Animalia")
        assertEquals(d.aphiaId, 2L)

    test("GET /details for unknown name returns 404"):
        assertEquals(statusCode(uri"http://localhost/details/UnknownTaxon"), 404)
