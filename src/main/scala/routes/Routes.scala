package routes
import actors.{Task, TaskActor}
import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}

import scala.concurrent.Future
import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.{AnyEndpoint, _}
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

case class ErrorResponse(message: String)

class Routes(taskActor: ActorRef)(implicit system: ActorSystem, executionContext: ExecutionContextExecutor) {

  implicit val log: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val allTasksEndpoint: Endpoint[Unit, Unit, String, List[Task], Any] = endpoint.get
    .in("tasks")
    .errorOut(stringBody)
    .out(jsonBody[List[Task]])

  private val singleTaskEndpoint
  : Endpoint[Unit, Int,  (StatusCode, ErrorResponse), (StatusCode,Option[Task]), Any] = endpoint.get
    .in("tasks")
    .in(path[Int]("id"))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .out(statusCode.and(jsonBody[Option[Task]]))

  private val storeTaskEndpoint: Endpoint[
    Unit,
    Task,
    (StatusCode, ErrorResponse),
    (StatusCode, Task),
    Any
  ] = endpoint.post
    .in("tasks")
    .in(jsonBody[Task])
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .out(statusCode.and(jsonBody[Task]))

  private val taskDeleteEndpoint: Endpoint[
    Unit,
    Int,
    (StatusCode, ErrorResponse),
    (StatusCode, String),
    Any
  ] = endpoint.delete
    .in("tasks")
    .in(path[Int]("id"))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .out(statusCode.and(jsonBody[String]))

  private val getAllTasks: Route = AkkaHttpServerInterpreter().toRoute(
    allTasksEndpoint
      .serverLogic(_ => {
        val tasksFuture: Future[List[actors.Task]] = (taskActor ? TaskActor.GetTasks).mapTo[List[Task]]
        tasksFuture.map(Right(_)).recover { case ex => Left(ex.getMessage) }
      })
  )
  private val getSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    singleTaskEndpoint
      .serverLogic(taskId => {
        val taskFuture: Future[Option[Task]] = (taskActor ? TaskActor.GetTaskById(taskId)).mapTo[Option[Task]]

        taskFuture.map {
          case Some(task) => Right(StatusCode.Found, Some(task))
          case None => Right(StatusCode.NotFound,None)
        }.recover {
          case ex => Left((StatusCode.InternalServerError, ErrorResponse(ex.getMessage)))
        }

      })
  )
  private val deleteSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    taskDeleteEndpoint
      .serverLogic(taskId => {
        val taskFuture: Future[String] = (taskActor ? TaskActor.DeleteTaskById(taskId)).mapTo[String]


        taskFuture.map(Right(StatusCode.Accepted,_)).recover {
          case ex => Left((StatusCode.InternalServerError, ErrorResponse(ex.getMessage)))
        }
      })
  )

  private val storeSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    storeTaskEndpoint
      .serverLogic(task => {

        if (task.id <= 0) {
          Future.successful(
            Left(
              StatusCode.BadRequest -> ErrorResponse(
                "negative ids are not accepted"
              )
            )
          )
        } else {

          val taskFuture: Future[Task] = (taskActor ? TaskActor.AddTask(task)).mapTo[Task]
          taskFuture.map(Right(StatusCode.Accepted,_)).recover {
            case ex => Left((StatusCode.InternalServerError, ErrorResponse(ex.getMessage)))
          }

        }
      })
  )

  private val endpointsForDocs = List(
    allTasksEndpoint,
    singleTaskEndpoint,
    taskDeleteEndpoint,
    storeTaskEndpoint
  )


  val routes: Route = concat(
    getAllTasks,
    getSingleTask,
    deleteSingleTask,
    storeSingleTask,
    withSwaggerDocs(endpointsForDocs)
  )

  private def withSwaggerDocs(endpoints: List[AnyEndpoint]): server.Route = {
    AkkaHttpServerInterpreter().toRoute(
      SwaggerInterpreter().fromEndpoints[Future](endpoints, "My First Scala App", "1.0")
    )
  }
}


