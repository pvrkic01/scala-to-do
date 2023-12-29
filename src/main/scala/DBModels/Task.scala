package DBModels

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

case class Task(id: Option[Int] = None, title: String)

class Tasks(tag: Tag) extends Table[Task](tag, "tasks") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def title: Rep[String] = column[String]("title")
  def description: Rep[String] = column[String]("description")

  def * : ProvenShape[Task] = (id.?, title) <> (Task.tupled, Task.unapply)
}

val tasks = TableQuery[Tasks]
