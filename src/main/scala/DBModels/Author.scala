package DBModels

import domains.author.{Database => AuthorImplementation}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape
class AuthorsDbTable(tag: Tag) extends Table[AuthorImplementation](tag, "authors") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  private def name: Rep[String] = column[String]("name")
  private def surname: Rep[String] = column[String]("surname")

  def * : ProvenShape[AuthorImplementation] = (id.?, name,surname) <> (AuthorImplementation.tupled, AuthorImplementation.unapply)
}

object Authors {
  val authorsTable = TableQuery[AuthorsDbTable]
}


