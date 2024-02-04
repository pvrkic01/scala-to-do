package services

import domains.task.{Database => TaskImplementation}
import domains.task.{In => TaskIn, Out => TaskOutput}
import repositories.TaskRepository
import routes.ErrorResponse

import scala.concurrent.{ExecutionContextExecutor, Future}
import slick.jdbc.MySQLProfile.api._
import sttp.model.StatusCode

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskService (implicit dbConnection: Database, executionContext: ExecutionContextExecutor)  {
  private val taskRepo = new TaskRepository()

  def formatInTask(task: TaskIn): TaskImplementation = {

    val formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")

    val formattedTimestamp: Option[LocalDateTime] = task.deadline.map(deadline => {
      LocalDateTime.parse(deadline, formatter)
    })


    TaskImplementation(None,task.title, task.description, formattedTimestamp)
  }

  def formatOutTask(task: TaskImplementation): TaskOutput = {
    val formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")
    val formattedTimestamp = task.deadline.map(formatter.format)


    TaskOutput(task.id.get, task.title, task.description, formattedTimestamp)
  }

  def storeNewTask(task: TaskIn): Future[Either[(StatusCode, ErrorResponse), (StatusCode, Int)]] = {
    val maybeSaved = taskRepo.save(formatInTask(task))

    maybeSaved.map(Right(StatusCode.Accepted,_)).recover {
      case ex => Left((StatusCode.InternalServerError, ErrorResponse(ex.getMessage)))
    }
  }

  def deleteTaskById(id:Int): Future[Either[(StatusCode, ErrorResponse), (StatusCode, Int)]] = {
    val maybeDeleted = taskRepo.deleteById(id)

    maybeDeleted.map(_ => Right(StatusCode.Accepted, id)).recover {
      case ex => Left(StatusCode.InternalServerError, ErrorResponse(ex.getMessage))
    }
  }
  def findTaskById(id:Int): Future[Either[(StatusCode, ErrorResponse), (StatusCode, Option[TaskOutput])]] = {
    val maybeTask = taskRepo.findById(id)

    maybeTask.map {
      case Some(task) => Right(StatusCode.Accepted, Some(formatOutTask(task)))
      case None => Right(StatusCode.NotFound,None)
    }.recover {
      case ex => Left(StatusCode.InternalServerError, ErrorResponse(ex.getMessage))
    }
  }
  def findAllTasks(): Future[Either[String, List[TaskOutput]]] = {
    val maybeAllTasks = taskRepo.findAll()
    maybeAllTasks.map{
      tasks => Right(tasks.map(formatOutTask))
    }.recover { case ex => Left(ex.getMessage) }
  }
}
