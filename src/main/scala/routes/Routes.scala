package routes
import actors.TaskActor
import DBModels.TaskDB
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

import slick.jdbc.MySQLProfile.api._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt


import DBModels.Tasks.{ tasks => tasksDB }
case class ErrorResponse(message: String)

class Routes(implicit system: ActorSystem, executionContext: ExecutionContextExecutor, dbConnection: Database) {

  implicit val log: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val allTasksEndpoint: Endpoint[Unit, Unit, String, List[TaskDB], Any] = endpoint.get
    .in("tasks")
    .errorOut(stringBody)
    .out(jsonBody[List[TaskDB]])

  private val singleTaskEndpoint
  : Endpoint[Unit, Int,  (StatusCode, ErrorResponse), (StatusCode,Option[TaskDB]), Any] = endpoint.get
    .in("tasks")
    .in(path[Int]("id"))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .out(statusCode.and(jsonBody[Option[TaskDB]]))

  private val storeTaskEndpoint: Endpoint[
    Unit,
    TaskDB,
    (StatusCode, ErrorResponse),
    (StatusCode, Int),
    Any
  ] = endpoint.post
    .in("tasks")
    .in(jsonBody[TaskDB])
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

        val maybeAllTasks = dbConnection.run(tasksDB.result).map(_.toList)
        maybeAllTasks.map(Right(_)).recover { case ex => Left(ex.getMessage) }
      })
  )
  private val getSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    singleTaskEndpoint
      .serverLogic(taskId => {

        val query = tasksDB.filter(_.id === taskId).result.headOption
        val maybeTask = dbConnection.run(query)

        maybeTask.map {
          case Some(task) => Right(StatusCode.Accepted, Some(task))
          case None => Right(StatusCode.NotFound,None)
        }.recover {
          case ex => Left((StatusCode.InternalServerError, ErrorResponse(ex.getMessage)))
        }

      })
  )
  private val deleteSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    taskDeleteEndpoint
      .serverLogic(taskId => {

        val query = tasksDB.filter(_.id === taskId).delete
        val maybeDeleteTask = dbConnection.run(query)
        maybeDeleteTask.map(Right(StatusCode.Accepted,_)).recover {
          case ex => Left((StatusCode.InternalServerError, ErrorResponse(ex.getMessage)))
        }
      })
  )

  private val storeSingleTask: Route = AkkaHttpServerInterpreter().toRoute(
    storeTaskEndpoint
      .serverLogic(task => {

        val maybeNewTask = dbConnection.run(tasksDB returning tasksDB.map(_.id) += task)
        maybeNewTask.map(Right(StatusCode.Accepted,_)).recover {
          case ex => Left((StatusCode.InternalServerError, ErrorResponse(ex.getMessage)))
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


