package cs441.HW3.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import com.typesafe.config.{Config, ConfigFactory}
import cs441.HW3.Utilz.CreateLogger
import org.slf4j.Logger
import spray.json._
import spray.json.DefaultJsonProtocol

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random

case class Node(id: Int, valuableData: Boolean)
case class AllowedMovesResponse(allowedMoves: Set[Node], scores: Map[String, Int])
case class StartGameResponse(message: String)
case class MoveResponse(message: String)
case class StatusResponse(status: String)

object JsonFormats extends DefaultJsonProtocol {

  val logger: Logger = CreateLogger(this.getClass)
  implicit val nodeFormat: RootJsonFormat[Node] = jsonFormat2(Node)
  implicit val allowedMovesResponseFormat: RootJsonFormat[AllowedMovesResponse] = jsonFormat2(AllowedMovesResponse.apply)
  implicit val startGameResponseFormat: RootJsonFormat[StartGameResponse] = jsonFormat1(StartGameResponse.apply)
  implicit val moveResponseFormat: RootJsonFormat[MoveResponse] = jsonFormat1(MoveResponse.apply)
  implicit val statusResponseFormat: RootJsonFormat[StatusResponse] = jsonFormat1(StatusResponse.apply)

  implicit val startGameResponseUnmarshaller: Unmarshaller[HttpEntity, StartGameResponse] =
    Unmarshaller.stringUnmarshaller.map(_.parseJson.convertTo[StartGameResponse])

  implicit val allowedMovesResponseUnmarshaller: Unmarshaller[HttpEntity, AllowedMovesResponse] =
    Unmarshaller.stringUnmarshaller.map { data =>
      if (data.trim.startsWith("{") && data.trim.endsWith("}")) {
        try {
          data.parseJson.convertTo[AllowedMovesResponse]
        } catch {
          case e: DeserializationException =>
            logger.error(s"Failed to parse JSON: $data")
            AllowedMovesResponse(Set.empty, Map.empty)
        }
      } else {
        logger.warn(s"Unexpected response format: $data")
        AllowedMovesResponse(Set.empty, Map.empty)
      }
    }

  implicit val moveResponseUnmarshaller: Unmarshaller[HttpEntity, MoveResponse] =
    Unmarshaller.stringUnmarshaller.map(_.parseJson.convertTo[MoveResponse])

  implicit val statusResponseUnmarshaller: Unmarshaller[HttpEntity, String] =
    Unmarshaller.stringUnmarshaller.map { data =>
      if (data.trim.startsWith("{") && data.trim.endsWith("}")) {
        val json = data.parseJson.asJsObject
        json.fields.get("status") match {
          case Some(JsString(status)) => status
          case _ => json.fields.get("message").map(_.toString).getOrElse("Unknown response format")
        }
      } else {
        data
      }
    }
}

object GameClient extends App {

  val logger: Logger = CreateLogger(this.getClass)
  implicit val system: ActorSystem = ActorSystem("GameClientSystem")
  implicit val executionContext: ExecutionContext = system.dispatcher
  import JsonFormats._

  private val serverUrl = "http://localhost:8080"
  private val config: Config = ConfigFactory.load("application.conf")
  private val strategy = config.getString("GraphPTGame.game.strategy")

  def startGame(): Future[String] = {
    val responseFuture = Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"$serverUrl/game/start"))
    responseFuture.flatMap(res => Unmarshal(res.entity).to[String])
  }

  private def getAllowedMoves(playerType: String): Future[AllowedMovesResponse] = {
    val responseFuture = Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = s"$serverUrl/game/$playerType/allowedMoves"))
    responseFuture.flatMap(res => Unmarshal(res.entity).to[AllowedMovesResponse])
  }

  private def makeMove(playerType: String, move: Int): Future[String] = {
    val responseFuture = Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"$serverUrl/game/move/$playerType/$move"))
    responseFuture.flatMap(res => Unmarshal(res.entity).to[String])
  }

  def getGameStatus: Future[String] = {
    val responseFuture = Http().singleRequest(HttpRequest(method = HttpMethods.GET, uri = s"$serverUrl/game/status"))
    responseFuture.flatMap(res => Unmarshal(res.entity).to[String])
  }

  private def maxScoreMove(allowedMovesResponse: AllowedMovesResponse): Int = {
    val maxScore = allowedMovesResponse.scores.values.max
    val bestMoveNodes = allowedMovesResponse.scores.collect {
      case (nodeId, score) if score == maxScore => allowedMovesResponse.allowedMoves.find(_.id == nodeId.toInt).get
    }.toSet
    randomChoiceMove(bestMoveNodes)
  }

  private def randomChoiceMove(moves: Set[Node]): Int = {
    Random.shuffle(moves.toList).headOption.map(_.id).getOrElse(-1)
  }

  private def chooseMove(allowedMovesResponse: AllowedMovesResponse): Int = {
    strategy match {
      case "1" => randomChoiceMove(allowedMovesResponse.allowedMoves)
      case "2" => maxScoreMove(allowedMovesResponse)
      case _ => throw new IllegalArgumentException(s"Unknown strategy: $strategy")
    }
  }

  private def gameLoop(): Future[Unit] = {
    for {
      statusResponseBeforeMove <- getGameStatus
      _ = logger.info(s"Initial $statusResponseBeforeMove")
      policemanMoves <- getAllowedMoves("policeman")
      chosenPolicemanMove = chooseMove(policemanMoves)
      _ <- makeMove("policeman", chosenPolicemanMove)
      statusResponseAfterPolicemanMove <- getGameStatus
      _ = logger.info(s"$statusResponseAfterPolicemanMove")
      thiefMoves <- getAllowedMoves("thief")
      chosenThiefMove = chooseMove(thiefMoves)
      _ <- makeMove("thief", chosenThiefMove)
      statusResponseAfterThiefMove <- getGameStatus
      _ = logger.info(s" $statusResponseAfterThiefMove")
      _ = Thread.sleep(1000) // Wait before next iteration
      result <- if (statusResponseAfterThiefMove.contains("Wins!") || statusResponseAfterThiefMove.contains("Game is over")) Future.successful(()) else gameLoop()
    } yield result
  }

  Await.result(startGame().flatMap(_ => gameLoop()), Duration.Inf)
  system.terminate()
}
