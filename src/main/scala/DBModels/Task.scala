package DBModels

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

case class TaskDB(id: Option[Int] = None, title: String, description: Option[String])

class Tasks(tag: Tag) extends Table[TaskDB](tag, "tasks") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  private def title: Rep[String] = column[String]("title")
  private def description: Rep[Option[String]] = column[Option[String]]("description")

  def * : ProvenShape[TaskDB] = (id.?, title,description) <> (TaskDB.tupled, TaskDB.unapply)
}

object Tasks {
  val tasks = TableQuery[Tasks]
}


