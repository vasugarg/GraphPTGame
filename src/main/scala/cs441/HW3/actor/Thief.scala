package cs441.HW3.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import cs441.HW3.models.{Edge, Node, Graph}
import cs441.HW3.messages._
import com.google.common.graph.MutableValueGraph

import scala.jdk.CollectionConverters._

object Thief {
  sealed trait Command
  case class Move(toNodeId: Int, replyTo: ActorRef[MoveResponse]) extends Command
  case class GetAllowedMoves(replyTo: ActorRef[AllowedMovesResponse]) extends Command
  case class GetPosition(replyTo: ActorRef[PositionResponse]) extends Command

  def apply(currentPosition: Node,
            perturbedGraph: MutableValueGraph[Node, Edge],
            originalGraph: MutableValueGraph[Node, Edge],
            gameManager: ActorRef[GameManager.Command],
            invalidMove: Boolean = false): Behavior[Command] = Behaviors.receive { (context, message) =>

    message match {
      case Move(toNodeId, replyTo) =>
        if (!originalGraph.nodes().asScala.exists(_.id == toNodeId)) {
          replyTo ! MoveResponse(isValid = false, "Invalid move: Node does not exist in the original graph")
          apply(currentPosition, perturbedGraph, originalGraph, gameManager, invalidMove = true)
        } else if (!Graph.isAdjacent(currentPosition, toNodeId, originalGraph)) {
          replyTo ! MoveResponse(isValid = false, "Invalid move: Node is not adjacent")
          Behaviors.same
        } else {
          val newPosition = originalGraph.nodes().asScala.find(_.id == toNodeId).getOrElse(currentPosition)
          replyTo ! MoveResponse(isValid = true, s"Moved to node $toNodeId")
          apply(newPosition, perturbedGraph, originalGraph, gameManager, invalidMove = false)
        }

      case GetAllowedMoves(replyTo) =>
        val allowedMoves = Graph.getAdjacentNodes(currentPosition, perturbedGraph)
        val scores = Graph.calculateConfidenceScore(originalGraph, perturbedGraph, allowedMoves)
        replyTo ! AllowedMovesResponse(allowedMoves, scores)
        Behaviors.same

      case GetPosition(replyTo) =>
        replyTo ! PositionResponse(currentPosition, invalidMove)
        Behaviors.same
    }
  }
}