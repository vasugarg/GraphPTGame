//import akka.actor.ActorSystem
//import akka.http.scaladsl.model.ws.{Message, TextMessage}
//import akka.http.scaladsl.server.Directives
//import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
//import akka.stream.scaladsl.Flow
//import org.scalatest.funsuite.AnyFunSuite
//import org.scalatest.matchers.should.Matchers
//
//class ServerTestScala extends AnyFunSuite with Matchers with ScalatestRouteTest {
//  test("should create empty GameService") {
//    new GameService()
//  }
//
//  test("should be able to connect to the GameService socket") {
//    val gameService = new GameService()
//    val wsClient = WSProbe()
//
//    WS("/", wsClient.flow) ~> gameService.websocketRoute ~> check {
//      isWebSocketUpgrade shouldBe true
//    }
//  }
//
//  test("should respond with correct message") {
//    val gameService = new GameService
//    val wsClient = WSProbe()
//
//    WS("/", wsClient.flow) ~> gameService.websocketRoute ~> check {
//      wsClient.sendMessage(TextMessage("Hello"))
//      wsClient.expectMessage() shouldEqual TextMessage("Hello")
//    }
//  }
//}
//
//class GameService() extends Directives {
//  implicit val system: ActorSystem = ActorSystem("game")
//
//  val websocketRoute = get {
//    handleWebSocketMessages(greeter)
//  }
//
//  def greeter: Flow[Message, Message, Any] =
//    Flow[Message].collect {
//      case tm: TextMessage.Strict => TextMessage(tm.text)
//    }
//}
