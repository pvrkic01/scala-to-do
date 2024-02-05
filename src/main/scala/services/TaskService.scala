package services

import domains.author.{Database => AuthorImplementation}
import domains.task.TaskWithAuthor.WithAuthor
import domains.task.{OutWithAuthor, Database => TaskImplementation, In => TaskIn, Out => TaskOutput}
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


    TaskImplementation(None,task.author,task.title, task.description, formattedTimestamp)
  }

  def formatOutTaskWithAuthor(task: TaskImplementation, author: AuthorImplementation): OutWithAuthor = {
    val formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")
    val formattedTimestamp = task.deadline.map(formatter.format)


    (TaskOutput(task.id.get, task.title, task.description, formattedTimestamp)).withAuthor(author)
  }

  def formatOutTask(task: TaskImplementation):TaskOutput  = {
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
  def findTaskById(id:Int): Future[Either[(StatusCode, ErrorResponse), (StatusCode, Option[OutWithAuthor])]] = {
    val maybeTask = taskRepo.findByIdWithAuthor(id)

    maybeTask.map {
      case Some((task,author)) => Right(StatusCode.Accepted, Some(formatOutTaskWithAuthor(task, author)))
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
