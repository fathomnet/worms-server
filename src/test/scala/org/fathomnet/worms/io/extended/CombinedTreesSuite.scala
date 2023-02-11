package org.fathomnet.worms.io.extended

import org.fathomnet.worms.WormsNode
import java.nio.file.Paths

class CombinedTreesSuite extends munit.FunSuite:

  test("incrementAphiaId") {
    val node = WormsNode("foo", "", 1000, Nil, Nil)
    val n = CombineTrees.incrementAphiaId(node, 1000)
    assertEquals(n.aphiaId, 2000L)
  }

  test("add") {
    val node1 = WormsNode("foo", "", 1000, Nil, Nil)
    val node2 = WormsNode("bar", "", 2000, Nil, Nil)
    val n = CombineTrees.add(node1, node2, 1000)
    assertEquals(n.aphiaId, 1000L)
    assertEquals(n.children.size, 1)
    assertEquals(n.children(0).aphiaId, 3000L)
    assertEquals(n.children(0).name, "bar")
  }

  test("combine") {
    val sample1 = getClass.getResource("/extended_tree_sample1.csv").getPath
    val sample2 = getClass.getResource("/extended_tree_sample2.csv").getPath
    val opt1 = ExtendedLoader.load(Paths.get(sample1))
    val opt2 = ExtendedLoader.load(Paths.get(sample2))
    assert(opt1.isDefined)
    assert(opt2.isDefined)
    val node1 = opt1.get
    val node2 = opt2.get

    val root = WormsNode("object", "", 0L, Nil, Nil)
    val combined = CombineTrees.combine(root, Seq(node1, node2))
    assertEquals(combined.aphiaId, 0L)
    assertEquals(combined.children.size, 2)
    
    val a0 = node1.find("Odor Pump")
    val a1 = combined.find("Odor Pump")
    assert(a0.isDefined)
    assert(a1.isDefined)
    assertEquals(a1.get.aphiaId, a0.get.aphiaId)

    val b0 = node2.find("Gonatus")
    val b1 = combined.find("Gonatus")
    assert(b0.isDefined)
    assert(b1.isDefined)
    assertEquals(b1.get.aphiaId, b0.get.aphiaId + node1.maxAphiaId)
    println(combined)
  }