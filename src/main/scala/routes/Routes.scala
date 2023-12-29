package routes
import actors.{Task, TaskActor}
import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.pattern.ask
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

trait TaskJsonProtocol extends DefaultJsonProtocol {
  implicit val taskFormat: RootJsonFormat[Task] = jsonFormat3(Task)
}



class Routes(taskActor: ActorRef)(implicit system: ActorSystem, executionContext: ExecutionContextExecutor)
  extends TaskJsonProtocol {

  implicit val log: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val exceptionHandler = ExceptionHandler {
    case ex: Exception =>
      log.error(ex, "Internal server error: {}", ex.getMessage)
      complete(StatusCodes.InternalServerError, "Internal server error occurred")
  }


  val routes: Route =
    handleExceptions(exceptionHandler) {pathPrefix("tasks") {
      pathEndOrSingleSlash {
        get {
          onSuccess((taskActor ? TaskActor.GetTasks).mapTo[List[Task]]) { tasks =>
            complete(HttpEntity(ContentTypes.`application/json`, tasks.toJson.toString))
          }
        }
      } ~
        path(IntNumber) { taskId =>
          get {
            onSuccess((taskActor ? TaskActor.GetTaskById(taskId)).mapTo[Option[Task]]) {
              case Some(task) => complete(HttpEntity(ContentTypes.`application/json`, task.toJson.toString))
              case None       => complete(StatusCodes.NotFound)
            }
          } ~
          delete {
            onSuccess((taskActor ? TaskActor.DeleteTaskById(taskId)).mapTo[String]) { message =>
              complete(StatusCodes.Accepted, message)
            }
          }
        } ~
        post {
          entity(as[Task]) { task =>
            onSuccess((taskActor ? TaskActor.AddTask(task)).mapTo[String]) { message =>
              complete(StatusCodes.Created, message)
            }
          }
        }
    }
  }
}
