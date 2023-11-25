package cs441.HW3.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Success, Try}

class GameAppSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("GameAppSpec")
  implicit val executionContext: ExecutionContext = system.dispatcher // Provide an ExecutionContext

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "The GameApp" should {
    "successfully bind to the specified port" in {
      val port = 8081
      val bindingFuture: Future[Http.ServerBinding] =
        Http().newServerAt("0.0.0.0", port).bind(Directives.complete(StatusCodes.OK))

      Try(Await.result(bindingFuture, Duration.Inf)) match {
        case Success(binding) =>
          binding.localAddress.getPort shouldBe port
          binding.unbind().onComplete { _ =>
            system.terminate()
          }
        case _ =>
          fail("Failed to bind HTTP endpoint")
      }
    }
  }
}
