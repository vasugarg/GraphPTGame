package cs441.HW3.models

import org.scalatest.funsuite.AnyFunSuite
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}

class GraphTest extends AnyFunSuite {

  def createTestGraph(): MutableValueGraph[Node, Edge] = {
    val graph = ValueGraphBuilder.directed().build[Node, Edge]()
    val node1 = Node(1, true)
    val node2 = Node(2, false)
    val node3 = Node(3, true)
    val edge1 = Edge(node1, node2, 1.0)
    val edge2 = Edge(node2, node3, 2.0)

    graph.addNode(node1)
    graph.addNode(node2)
    graph.addNode(node3)
    graph.putEdgeValue(node1, node2, edge1)
    graph.putEdgeValue(node2, node3, edge2)

    graph
  }

  test("loadGraph should load a graph from a file") {
    val filePath = "inputs/NetGraph_14-11-23-20-29-30.ngs"
    val graph = Graph.loadGraph(filePath)
    assert(graph.nodes().size() > 0)
    assert(graph.edges().size() > 0)
  }

  test("getAdjacentNodes should return adjacent nodes") {
    val graph = createTestGraph()
    val node1 = Node(1, true)
    val adjacentNodes = Graph.getAdjacentNodes(node1, graph)
    assert(adjacentNodes.size == 1)
    assert(adjacentNodes.contains(Node(2, false)))
  }


  test("calculateConfidenceScore should calculate confidence scores") {
    val originalGraph = createTestGraph()
    val perturbedGraph = createTestGraph()

    perturbedGraph.removeEdge(Node(2, false), Node(3, true))

    val adjacentNodes = Set(Node(2, false), Node(3, true))
    val confidenceScores = Graph.calculateConfidenceScore(originalGraph, perturbedGraph, adjacentNodes)
    assert(confidenceScores.size == 2)
    assert(confidenceScores("2") == 50)
    assert(confidenceScores("3") == 0)
  }


}

