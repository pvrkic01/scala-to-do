package repositories

import DBModels.Authors.authorsTable
import DBModels.Tasks
import DBModels.Tasks.{tasks => tasksDB}
import domains.task.{Database => TaskImplementation}
import domains.author.{Database => AuthorImplementation}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Query

import scala.concurrent.{ExecutionContextExecutor, Future}


class TaskRepository(implicit dbConnection: Database, executionContext: ExecutionContextExecutor) extends Repository[TaskImplementation, Int]{

  implicit class TaskExtensions[C[_]](q: Query[Tasks, TaskImplementation, C]) {
    // specify mapping of relationship to address
    def withAuthor = q.join(authorsTable).on(_.authorId === _.id)
  }
  override def save(entity: TaskImplementation): Future[Int] = {
    dbConnection.run(tasksDB returning tasksDB.map(_.id) += entity)
  }

  override def deleteById(id: Int): Future[Int] = {
    dbConnection.run(tasksDB.filter(_.id === id).delete)
  }
  override def findById(id: Int): Future[Option[TaskImplementation]] = {
    dbConnection.run(tasksDB.filter(_.id === id).result.headOption)
  }

  def findByIdWithAuthor(id: Int): Future[Option[(TaskImplementation,AuthorImplementation)]] = {
    dbConnection.run(tasksDB.filter(_.id === id).withAuthor.result.headOption)
  }

  override def findAll(): Future[List[TaskImplementation]] = {
    dbConnection.run(tasksDB.result).map(_.toList)
  }
}
