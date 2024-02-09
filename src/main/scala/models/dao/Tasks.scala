package models.dao

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import Authors_.authorsTable
import models.dao.{Task => TaskImplementation}

class Tasks(tag: Tag) extends Table[Task](tag, "tasks") {

  implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
    str => LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
  )
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def authorId = column[Int]("author_id")
  def author_fk = foreignKey("author_fk", authorId, authorsTable)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)
  private def title: Rep[String] = column[String]("title")
  private def description: Rep[Option[String]] = column[Option[String]]("description")
  private def deadline: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("deadline")

  def * : ProvenShape[Task] = (id.?, authorId,title,description,deadline) <> (TaskImplementation.tupled, TaskImplementation.unapply)
}

object Tasks_ {
  val tasks = TableQuery[Tasks]
}


