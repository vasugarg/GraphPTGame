package cs441.HW3.models

import scala.io.Source
import scala.jdk.CollectionConverters._
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import cs441.HW3.Utilz.CreateLogger
import io.circe.generic.auto._
import io.circe.parser._
import org.slf4j.Logger

case class Node(id:Int,
                valuableData:Boolean)
case class Edge(fromNode: Node, toNode: Node, cost: Double)

object Graph {

  val logger: Logger = CreateLogger(this.getClass)

  def loadGraph(filePath: String): MutableValueGraph[Node, Edge] = {

    val lines = Source.fromFile(filePath).getLines().toList
    val nodesJson = lines.head
    val edgesJson = lines(1)

    val nodes: Seq[Node] = decode[Seq[Node]](nodesJson) match {
      case Right(value) => value
      case Left(error) =>
        logger.error(s"Error parsing nodes JSON: $error")
        Seq.empty[Node]
    }

    val edges: Seq[Edge] = decode[Seq[Edge]](edgesJson) match {
      case Right(value) => value
      case Left(error) =>
        logger.error(s"Error parsing edges JSON: $error")
        Seq.empty[Edge]
    }

    val graph: MutableValueGraph[Node, Edge] = ValueGraphBuilder.directed().build()
    nodes.foreach(node => graph.addNode(node))
    edges.foreach(edge => graph.putEdgeValue(edge.fromNode, edge.toNode, edge)) // Replace null with actual edge value if needed

    graph
  }

  def getAdjacentNodes(node: Node, graph: MutableValueGraph[Node, Edge]): Set[Node] = {
    graph.adjacentNodes(node).asScala.toSet
  }

  def isAdjacent(currentPosition: Node, toNodeId: Int, graph: MutableValueGraph[Node, Edge]): Boolean = {
    graph.adjacentNodes(currentPosition).asScala.exists(_.id == toNodeId)
  }

  /**
   * Calculates a confidence score for each node in the set of adjacent nodes
   * from the perturbed graph by checking the nodes in the original graph.
   *
   * The scores are calculated based on the intersection of adjacent nodes.
   *
   * @param originalGraph  The original graph.
   * @param perturbedGraph The perturbed graph.
   * @param adjacentNodes  A set of adjacent nodes from the perturbed graph.
   * @return A Map from Node ID to its calculated confidence score as an Int.
   */
  def calculateConfidenceScore(originalGraph: MutableValueGraph[Node, Edge],
                               perturbedGraph: MutableValueGraph[Node, Edge],
                               adjacentNodes: Set[Node]): Map[String, Int] = {
    adjacentNodes.map { node =>
      val score = calculateNodeScore(originalGraph, perturbedGraph, node)
      node.id.toString -> (score * 100).toInt // Convert to percentage and then to Int
    }.toMap
  }

  private def calculateNodeScore(originalGraph: MutableValueGraph[Node, Edge],
                                 perturbedGraph: MutableValueGraph[Node, Edge],
                                 node: Node): Double = {
    val originalAdjacentNodes = if (originalGraph.nodes().contains(node)) originalGraph.adjacentNodes(node).asScala.toSet else Set.empty[Node]
    val perturbedAdjacentNodes = perturbedGraph.adjacentNodes(node).asScala.toSet

    val commonEdges = originalAdjacentNodes.intersect(perturbedAdjacentNodes).size
    val totalEdges = originalAdjacentNodes.size.max(perturbedAdjacentNodes.size)

    if (totalEdges > 0) commonEdges.toDouble / totalEdges else 1.0
  }
}





