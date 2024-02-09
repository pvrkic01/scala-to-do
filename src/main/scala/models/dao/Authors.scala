package models.dao

import models.dao.{Author => AuthorImplementation}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape
class Authors(tag: Tag) extends Table[Author](tag, "authors") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  private def name: Rep[String] = column[String]("name")
  private def surname: Rep[String] = column[String]("surname")

  def * : ProvenShape[Author] = (id.?, name,surname) <> (AuthorImplementation.tupled, AuthorImplementation.unapply)
}

object Authors_ {
  val authorsTable = TableQuery[Authors]
}


