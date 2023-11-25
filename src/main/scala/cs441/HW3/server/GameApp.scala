package cs441.HW3.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import cs441.HW3.actor.GameManager
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object GameApp extends App {

  sealed trait Message
  private val rootBehavior = Behaviors.setup[Message] { context =>

    val gameManager = context.spawn(GameManager(), "gameManager")
    val gameServer = new GameServer(context.system, gameManager)
    val routes: Route = gameServer.routes
    startHttpServer(routes)(context.system)

    Behaviors.empty
  }

  val system: ActorSystem[Message] = ActorSystem[Message](rootBehavior, "gameSystem")

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    implicit val executionContext: ExecutionContext = system.executionContext

    val futureBinding = Http().newServerAt("0.0.0.0", 8081).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
    // Await.result(system.whenTerminated, Duration.Inf)
  }
}