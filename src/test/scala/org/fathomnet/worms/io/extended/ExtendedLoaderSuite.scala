/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.io.extended

import java.nio.file.Paths

class ExtendedLoaderSuite extends munit.FunSuite:

  private val sample = getClass.getResource("/extended_tree_sample1.csv").getPath
  
  test("from"):
    val a = "1,2,foo;bar;baz bot"
    val opt = ExtendedLoader.from(a)
    assert(opt.isDefined)
    val e = opt.get
    assertEquals(e.id, 1L)
    assertEquals(e.parentId, Some(2L))
    assertEquals(e.names.size, 3)
    assertEquals(e.names(0).name, "foo")
    assertEquals(e.names(0).isPrimary, true)
    assertEquals(e.names(1).name, "bar")
    assertEquals(e.names(1).isPrimary, false)
    assertEquals(e.names(2).name, "baz bot")
    assertEquals(e.names(2).isPrimary, false)

  test("read"):
    val wormsConcepts = ExtendedLoader.read(sample)
    assertEquals(wormsConcepts.size, 10)

  test("load"):
    val opt = ExtendedLoader.load(Paths.get(sample))
    assert(opt.isDefined)
    val worms = opt.get
    assertEquals(worms.aphiaId, 1000L)
    assertEquals(worms.name, "equipment")
    assertEquals(worms.rank, "")
    assertEquals(worms.children.size, 7)
    println(worms.descendantNames)
    assertEquals(worms.descendantNames.size, 10)
