package cs441.HW3.messages

import akka.actor.typed.ActorRef
import cs441.HW3.models.Node

sealed trait Command

case class StartGameResponse(message: String)
case class GetPosition(replyTo: ActorRef[PositionResponse]) extends Command
case class PositionResponse(node: Node, invalidMove: Boolean)
final case class MoveResponse(isValid: Boolean, message: String)
case class StatusResponse(status: String)
case class AllowedMovesResponse(allowedMoves: Set[Node], scores: Map[String, Int])




