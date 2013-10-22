package org.apache.spark.graph

import org.scalatest.FunSuite

import org.apache.spark.SparkContext
import org.apache.spark.graph.LocalSparkContext._

/*
  GraphSuite is a class implement interface  LocalSparkContext and is a subclass of FunSuite
 */
class GraphSuite extends FunSuite with LocalSparkContext {
  /*
  set the property of the class ,to a certain degree,just like the global static variables
   */
  System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  System.setProperty("spark.kryo.registrator", "org.apache.spark.graph.GraphKryoRegistrator")

  /*
  this is a test unit,sc=> is a part of the specific grammar of test framework
   */
  test("Graph Creation") {
    withSpark(new SparkContext("local", "test")) { sc =>
      val rawEdges = (0L to 100L).zip((1L to 99L) :+ 0L)
      val edges = sc.parallelize(rawEdges)
      val graph = Graph(edges)
      assert( graph.edges.count() === rawEdges.size )
    }
  }

  test("mapEdges") {
    withSpark(new SparkContext("local", "test")) { sc =>
      val n = 3
      val star = Graph(sc.parallelize((1 to n).map(x => (0: Vid, x: Vid))))
      val starWithEdgeAttrs = star.mapEdges(e => e.dstId)

      // map(_.copy()) is a workaround for https://github.com/amplab/graphx/issues/25
      val edges = starWithEdgeAttrs.edges.map(_.copy()).collect()
      assert(edges.size === n)
      assert(edges.toSet === (1 to n).map(x => Edge(0, x, x)).toSet)
    }
  }

  test("aggregateNeighbors") {
    withSpark(new SparkContext("local", "test")) { sc =>
      val n = 3
      val star = Graph(sc.parallelize((1 to n).map(x => (0: Vid, x: Vid))))

      val indegrees = star.aggregateNeighbors(
        (vid, edge) => Some(1),
        (a: Int, b: Int) => a + b,
        EdgeDirection.In)
      assert(indegrees.collect().toSet === (1 to n).map(x => (x, 1)).toSet)

      val outdegrees = star.aggregateNeighbors(
        (vid, edge) => Some(1),
        (a: Int, b: Int) => a + b,
        EdgeDirection.Out)
      assert(outdegrees.collect().toSet === Set((0, n)))

      val noVertexValues = star.aggregateNeighbors[Int](
        (vid: Vid, edge: EdgeTriplet[Int, Int]) => None,
        (a: Int, b: Int) => throw new Exception("reduceFunc called unexpectedly"),
        EdgeDirection.In)
      assert(noVertexValues.collect().toSet === Set.empty[(Vid, Int)])
    }
  }


  /*

   */
  test("joinVertices") {
    withSpark(new SparkContext("local", "test")) { sc =>
      val vertices = sc.parallelize(Seq[(Vid, String)]((1, "one"), (2, "two"), (3, "three")), 2)
      val edges = sc.parallelize((Seq(Edge(1, 2, "onetwo"))))
      val g: Graph[String, String] = Graph(vertices, edges)

      val tbl = sc.parallelize(Seq[(Vid, Int)]((1, 10), (2, 20)))
      val g1 = g.joinVertices(tbl) { (vid: Vid, attr: String, u: Int) => attr + u }

      val v = g1.vertices.collect().toSet
      assert(v === Set((1, "one10"), (2, "two20"), (3, "three")))
    }
  }

//  test("graph partitioner") {
//    sc = new SparkContext("local", "test")
//    val vertices = sc.parallelize(Seq(Vertex(1, "one"), Vertex(2, "two")))
//    val edges = sc.parallelize(Seq(Edge(1, 2, "onlyedge")))
//    var g = Graph(vertices, edges)
//
//    g = g.withPartitioner(4, 7)
//    assert(g.numVertexPartitions === 4)
//    assert(g.numEdgePartitions === 7)
//
//    g = g.withVertexPartitioner(5)
//    assert(g.numVertexPartitions === 5)
//
//    g = g.withEdgePartitioner(8)
//    assert(g.numEdgePartitions === 8)
//
//    g = g.mapVertices(x => x)
//    assert(g.numVertexPartitions === 5)
//    assert(g.numEdgePartitions === 8)
//
//    g = g.mapEdges(x => x)
//    assert(g.numVertexPartitions === 5)
//    assert(g.numEdgePartitions === 8)
//
//    val updates = sc.parallelize(Seq((1, " more")))
//    g = g.updateVertices(
//      updates,
//      (v, u: Option[String]) => if (u.isDefined) v.data + u.get else v.data)
//    assert(g.numVertexPartitions === 5)
//    assert(g.numEdgePartitions === 8)
//
//    g = g.reverse
//    assert(g.numVertexPartitions === 5)
//    assert(g.numEdgePartitions === 8)
//
//  }
}
