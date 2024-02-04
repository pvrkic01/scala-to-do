import akka.http.scaladsl.Http
import akka.stream.Materializer
import routes.Routes

import akka.actor.{ActorSystem, Props, ActorRef}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import slick.jdbc.MySQLProfile.api._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("todoListSystem")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val dbConnection: Database = Database.forConfig("mysql")

  private val taskRoutes = new Routes()

  private val bindingFuture = Http().newServerAt("localhost", 8089).bindFlow(taskRoutes.routes)

  println("Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
