package actors
import DBModels.Tasks.{ tasks => tasksDB }
import DBModels.TaskDB
import akka.actor.{Actor, ActorLogging}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContextExecutor, Future}
case class Task(id: Int, title: String, description: String = "")

object TaskActor {
  case class GetTasks()
  case class GetTaskByIdDB(taskId: Int)
  case class DeleteTaskByIdDB(taskId: Int)
  case class AddTaskDB(task: TaskDB)
}

class TaskActor (db: Database)(implicit executionContext: ExecutionContextExecutor) extends Actor with ActorLogging {
  import TaskActor._

  def getAllTasks: Future[List[TaskDB]] =
    db.run(tasksDB.result).map(_.toList)

  def addTask(task: TaskDB): Future[Int] =
    db.run(tasksDB returning tasksDB.map(_.id) += task)

  def getTaskById(id: Int): Future[Option[TaskDB]] = {
    val query = tasksDB.filter(_.id === id).result.headOption
    db.run(query)
  }
  def deleteTaskById(id: Int): Future[Int] = {
    val query = tasksDB.filter(_.id === id).delete
    db.run(query)
  }


  override def receive: Receive = {
    case GetTasks =>
      val senderRef = sender()
      getAllTasks.onComplete {
        case scala.util.Success(tasksDb) => senderRef ! tasksDb
        case scala.util.Failure(ex) => senderRef ! akka.actor.Status.Failure(ex)
      }
    case GetTaskByIdDB(taskId) =>
      val senderRef = sender()
      val result: Future[Option[TaskDB]] = getTaskById(taskId)
      result.map { taskOpt =>
        senderRef ! taskOpt // Slanje rezultata dohvatanja zadatka
      }.recover {
        case ex =>
          senderRef ! akka.actor.Status.Failure(ex)// Slanje odgovora u slučaju greške
      }
    case DeleteTaskByIdDB(taskId) =>
      val senderRef = sender() // Referenca na pošiljaoca poruke

      val result: Future[Int] = deleteTaskById(taskId)
      result.map { rowsAffected =>
        if (rowsAffected > 0) {
          senderRef ! "Task removed successfully" // Slanje potvrde o brisanju zadatka
        } else {
          senderRef ! "Task not found" // Slanje poruke da zadatak nije pronađen
        }
      }.recover {
        case ex =>
          senderRef ! akka.actor.Status.Failure(ex) // Slanje odgovora u slučaju greške
      }

    case AddTaskDB(task) =>
      val senderRef = sender() // Referenca na pošiljaoca poruke
      val result: Future[Int] = addTask(task)
      result.map { id =>
        senderRef ! id // Slanje odgovora nakon uspešnog dodavanja zadatka
      }.recover {
        case ex =>
          senderRef ! ex.getMessage // Slanje odgovora u slučaju greške
      }
  }
}
