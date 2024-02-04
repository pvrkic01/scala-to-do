package repositories

import slick.jdbc.MySQLProfile.api._
import DBModels.Tasks.{tasks => tasksDB}
import domains.task.{Database => TaskImplementation}

import scala.concurrent.{ExecutionContextExecutor, Future}
class TaskRepository(implicit dbConnection: Database, executionContext: ExecutionContextExecutor) extends Repository[TaskImplementation, Int]{

  override def save(entity: TaskImplementation): Future[Int] = {
    dbConnection.run(tasksDB returning tasksDB.map(_.id) += entity)
  }

  override def deleteById(id: Int): Future[Int] = {
    dbConnection.run(tasksDB.filter(_.id === id).delete)
  }
  override def findById(id: Int): Future[Option[TaskImplementation]] = {
    dbConnection.run(tasksDB.filter(_.id === id).result.headOption)
  }

  override def findAll(): Future[List[TaskImplementation]] = {
    dbConnection.run(tasksDB.result).map(_.toList)
  }
}
