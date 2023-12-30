import actors.TaskActor
import akka.http.scaladsl.Http
import akka.stream.Materializer
import routes.Routes

import akka.actor.{ActorSystem, Props, ActorRef}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("todoListSystem")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val taskActor: ActorRef = system.actorOf(Props[TaskActor], "taskActor")
  private val taskRoutes = new Routes(taskActor)

  private val bindingFuture = Http().newServerAt("localhost", 8080).bindFlow(taskRoutes.routes)

  println("Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
