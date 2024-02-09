package routes
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.circe.generic.auto._
import models.api.in.Task
import models.api.out.{TaskWithAuthor, Task => TaskOutput}
import services.TaskService
import slick.jdbc.MySQLProfile.api._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.{AnyEndpoint, _}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt
case class ErrorResponse(message: String)

class Routes(implicit system: ActorSystem, executionContext: ExecutionContextExecutor, dbConnection: Database) {

  implicit val log: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val taskService = new TaskService()

  private val allTasksEndpoint: Endpoint[Unit, Unit, String, List[TaskOutput], Any] = endpoint.get
    .in("tasks")
    .errorOut(stringBody)
    .out(jsonBody[List[TaskOutput]])

  private val singleTaskEndpoint
  : Endpoint[Unit, Int,  (StatusCode, ErrorResponse ), (StatusCode,Option[TaskWithAuthor]), Any] = endpoint.get
    .in("tasks")
    .in(path[Int]("id"))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .out(statusCode.and(jsonBody[Option[TaskWithAuthor]]))

  private val storeTaskEndpoint: Endpoint[
    Unit,
    Task,
    (StatusCode, ErrorResponse),
    (StatusCode, Int),
    Any
  ] = endpoint.post
    .in("tasks")
    .in(jsonBody[Task])
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .out(statusCode.and(jsonBody[Int]))

  private val taskDeleteEndpoint: Endpoint[
    Unit,
    Int,
    (StatusCode, ErrorResponse),
    (StatusCode, Int),
    Any
  ] = endpoint.delete
    .in("tasks")
    .in(path[Int]("id"))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .out(statusCode.and(jsonBody[Int]))

  private val getAllTasks: Route = AkkaHttpServerInterpreter().toRoute(
    allTasksEndpoint
      .serverLogic(_ => {
        taskService.findAllTasks()
      })
  )
  private val getSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    singleTaskEndpoint
      .serverLogic(taskId => {

        taskService.findTaskById(taskId)

      })
  )
  private val deleteSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    taskDeleteEndpoint
      .serverLogic(taskId => {
        taskService.deleteTaskById(taskId)
      })
  )

  private val storeSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    storeTaskEndpoint
      .serverLogic(task => {
        taskService.storeNewTask(task)
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


