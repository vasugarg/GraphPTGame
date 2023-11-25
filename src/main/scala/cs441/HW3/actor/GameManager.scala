package cs441.HW3.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import cs441.HW3.models.{Edge, Node}
import com.google.common.graph.MutableValueGraph
import scala.jdk.CollectionConverters._
import scala.util.Random

object GameManager {
  sealed trait Command
  private sealed trait State

  private case object Uninitialized extends State
  private case class Initialized(policemanActor: ActorRef[Policeman.Command], thiefActor: ActorRef[Thief.Command]) extends State

  private case class CheckGameStatus(policemanPosition: Node, thiefPosition: Node, replyTo: ActorRef[GameStatus]) extends Command
  private case class GameStatus(gameOver: Boolean, winner: Option[String])
  case class InitializeGame(originalGraph: MutableValueGraph[Node, Edge], perturbedGraph: MutableValueGraph[Node, Edge], replyTo: ActorRef[GameInitialized]) extends Command
  case class GameInitialized(gameManager: ActorRef[GameManager.Command], policemanActor: ActorRef[Policeman.Command], thiefActor: ActorRef[Thief.Command], policemanPosition: Node, thiefPosition: Node) extends Command
  private case class InvalidMove(playerType: String, replyTo: ActorRef[GameStatus]) extends Command

  def apply(): Behavior[Command] = gameBehavior(Uninitialized)

  private def gameBehavior(state: State): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {
      case InitializeGame(originalGraph, perturbedGraph, replyTo) =>
        val policemanPosition = getRandomNode(originalGraph)
        var thiefPosition = getRandomNode(originalGraph)
        while (thiefPosition == policemanPosition || thiefPosition.valuableData) {
          thiefPosition = getRandomNode(originalGraph)
        }

        val policemanActor = context.spawn(Policeman(policemanPosition, perturbedGraph, originalGraph, context.self), "policemanActor")
        val thiefActor = context.spawn(Thief(thiefPosition, perturbedGraph, originalGraph, context.self), "thiefActor")

        replyTo ! GameInitialized(context.self, policemanActor, thiefActor, policemanPosition, thiefPosition)
        gameBehavior(Initialized(policemanActor, thiefActor))

      case CheckGameStatus(policemanPosition, thiefPosition, replyTo) =>
        val gameOver = policemanPosition == thiefPosition
        val winner = if (gameOver) Some("Policeman") else None
        replyTo ! GameStatus(gameOver, winner)
        Behaviors.same

      case InvalidMove(playerType, replyTo) =>
        val winner = playerType match {
          case "policeman" => "Thief"
          case "thief" => "Policeman"
        }
        replyTo ! GameStatus(gameOver = true, Some(winner))
        gameBehavior(state)

      case GameInitialized(_, _, _, _, _) =>
        Behaviors.same

    }
  }

  def getRandomNode(graph: MutableValueGraph[Node, Edge]): Node = {
    val nodes = graph.nodes().asScala.toSeq
    nodes(Random.nextInt(nodes.size))
  }
}
