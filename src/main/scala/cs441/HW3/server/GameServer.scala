package cs441.HW3.server

import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import cs441.HW3.Utilz.CreateLogger
import cs441.HW3.actor._
import cs441.HW3.models.{Graph, Node}
import cs441.HW3.messages._
import org.slf4j.Logger

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import spray.json.RootJsonFormat

import scala.util.{Failure, Success}

case class SimpleResponse(message: String)

class GameServer(system: ActorSystem[_],
                 gameManager: ActorRef[GameManager.Command]
                ) {
  val logger: Logger = CreateLogger(this.getClass)
  private val config: Config = ConfigFactory.load("application.conf")

  private var policemanActorOpt: Option[ActorRef[Policeman.Command]] = None
  private var thiefActorOpt: Option[ActorRef[Thief.Command]] = None
  private var isGameOver: Boolean = false

  def policemanActor: ActorRef[Policeman.Command] = policemanActorOpt.getOrElse(throw new IllegalStateException("Game not started"))
  def thiefActor: ActorRef[Thief.Command] = thiefActorOpt.getOrElse(throw new IllegalStateException("Game not started"))

  implicit val simpleResponseFormat: RootJsonFormat[SimpleResponse] = jsonFormat1(SimpleResponse)
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler

  private object JsonFormats {
    import spray.json.DefaultJsonProtocol._

    implicit val statusResponseFormat: RootJsonFormat[StatusResponse] = jsonFormat1(StatusResponse)
    implicit val nodeFormat: RootJsonFormat[Node] = jsonFormat2(Node)
    implicit val allowedMovesResponseFormat: RootJsonFormat[AllowedMovesResponse] = jsonFormat2(AllowedMovesResponse)
  }

  import JsonFormats._
  import akka.actor.typed.scaladsl.AskPattern._

  private def getGameStatus: Future[String] = {
    val policemanPositionFuture = policemanActor.ask(ref => Policeman.GetPosition(ref))(timeout, scheduler)
    val thiefPositionFuture = thiefActor.ask(ref => Thief.GetPosition(ref))(timeout, scheduler)

    for {
      policemanPosition <- policemanPositionFuture
      thiefPosition <- thiefPositionFuture
    } yield {
      val gameStatus = if (policemanPosition.node == thiefPosition.node) {
        isGameOver = true
        "Policeman Wins!"
      } else if (thiefPosition.node.valuableData) {
        isGameOver = true
        "Thief Wins!"
      } else {
        "Game in progress"
      }

      s"Game Status: $gameStatus | Policeman at Node: ${policemanPosition.node.id}, Thief at Node: ${thiefPosition.node.id}"
    }
  }

  val routes: Route = {
    concat(
      path("game" / "start") {
        post {
          onComplete(startGame()) {
            case Success(message) => complete(ToResponseMarshallable(SimpleResponse(message)))
            case Failure(exception) => complete(StatusCodes.InternalServerError, SimpleResponse(s"Failed to start game: ${exception.getMessage}"))
          }
        }
      },
      path("game" / "move" / Segment / Segment) { (playerType, move) =>
        post {
          if (isGameOver) {
            complete(StatusCodes.BadRequest, SimpleResponse("Game is over. No more moves allowed."))
          } else {
            val toNodeId: Int = move.toInt
            val moveResponseFuture = playerType.toLowerCase match {
              case "policeman" => policemanActor.ask(Policeman.Move(toNodeId, _))(timeout, scheduler)
              case "thief" => thiefActor.ask(Thief.Move(toNodeId, _))(timeout, scheduler)
              case _ => Future.successful(MoveResponse(isValid = false, "Invalid player type"))
            }

            onComplete(moveResponseFuture) {
              case Success(MoveResponse(true, _)) =>
                onComplete(getGameStatus) {
                  case Success(status) =>
                    if (status contains "Wins!") {
                      isGameOver = true
                    }
                    complete(StatusCodes.OK, s"$playerType moved to $move and $status")
                  case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                }
              case Success(MoveResponse(false, msg)) =>
                onComplete(getGameStatus) {
                  case Success(status) => complete(StatusCodes.BadRequest, s"$msg and $status")
                  case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                }
              case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          }
        }
      },
      path("game" / "status") {
        get {
          onComplete(getGameStatus) {
            case Success(status) =>
              if (status contains "Wins!") {
                isGameOver = true
              }
              complete(StatusResponse(status))
            case Failure(ex) => complete(StatusCodes.InternalServerError, SimpleResponse(s"An error occurred: ${ex.getMessage}"))
          }
        }
      },
      path("game" / Segment / "allowedMoves") { playerType =>
        get {
          if (isGameOver) {
            complete(StatusCodes.BadRequest, SimpleResponse("Game is over. No more moves allowed."))
          } else {
            val allowedMovesFuture = playerType match {
              case "policeman" => policemanActor.ask(ref => Policeman.GetAllowedMoves(ref))
              case "thief" => thiefActor.ask(ref => Thief.GetAllowedMoves(ref))
              case _ => Future.failed(new IllegalArgumentException("Invalid player type"))
            }

            onSuccess(allowedMovesFuture) {
              case AllowedMovesResponse(allowedMoves, scores) =>
                val scoresWithNodeIds = scores.map { case (node, score) => (node, score) }
                val response = AllowedMovesResponse(allowedMoves, scoresWithNodeIds)
                complete(ToResponseMarshallable(response))
              case _ =>
                complete(StatusCodes.BadRequest, "Invalid request")
            }
          }
        }
      }
    )
  }

  private def startGame(): Future[String] = {
    val originalGraphPath = sys.env.getOrElse("ORIGINAL_GRAPH_PATH", config.getString("GraphPTGame.FilePathsLocal.originalGraphPath"))
    val perturbedGraphPath = sys.env.getOrElse("PERTURBED_GRAPH_PATH",config.getString("GraphPTGame.FilePathsLocal.perturbedGraphPath"))
    val originalGraph = Graph.loadGraph(originalGraphPath)
    val perturbedGraph = Graph.loadGraph(perturbedGraphPath)

    gameManager.ask(GameManager.InitializeGame(originalGraph, perturbedGraph, _))(timeout, scheduler)
      .map {
        case GameManager.GameInitialized(gameMgr, policeman, thief, policemanPos, thiefPos) =>
          policemanActorOpt = Some(policeman)
          thiefActorOpt = Some(thief)
          s"Game started. Policeman is at Node: ${policemanPos.id}, Thief is at Node: ${thiefPos.id}"
      }.recover {
        case ex: Exception => s"Failed to start game: ${ex.getMessage}"
      }
  }
}