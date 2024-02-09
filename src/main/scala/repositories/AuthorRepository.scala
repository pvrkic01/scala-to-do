package repositories

import models.dao.{Authors_, Author => AuthorImplementation}
import models.dao.Authors_.authorsTable
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContextExecutor, Future}


class AuthorRepository(implicit dbConnection: Database, executionContext: ExecutionContextExecutor) extends Repository[AuthorImplementation, Int]{

  override def save(entity: AuthorImplementation): Future[Int] = {
    dbConnection.run(authorsTable returning authorsTable.map(_.id) += entity)
  }

  override def deleteById(id: Int): Future[Int] = {
    dbConnection.run(authorsTable.filter(_.id === id).delete)
  }
  override def findById(id: Int): Future[Option[AuthorImplementation]] = {
    dbConnection.run(authorsTable.filter(_.id === id).result.headOption)
  }

  override def findAll(): Future[List[AuthorImplementation]] = {
    dbConnection.run(authorsTable.result).map(_.toList)
  }
}
