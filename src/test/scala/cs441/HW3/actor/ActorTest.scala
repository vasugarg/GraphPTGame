package cs441.HW3.actor

import org.scalatest.funsuite.AnyFunSuite
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import cs441.HW3.models.{Edge, Node}

class ActorTest extends AnyFunSuite {

  test("getRandomNode should return a valid node from the graph") {
    // Create a sample graph
    val graph: MutableValueGraph[Node, Edge] = ValueGraphBuilder.directed().build()
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

    // Call getRandomNode method multiple times and assert that the returned node is in the graph
    for (_ <- 1 to 100) {
      val randomNode = GameManager.getRandomNode(graph)
      assert(graph.nodes().contains(randomNode))
    }
  }
}
