package models.api.out

import models.api.Author

case class TaskWithAuthor(id: Int, title: String, description: Option[String], deadline: Option[String], author: Author) extends BaseTaskOut

object TaskWithAuthor{
  def from(task: models.dao.Task, author: models.dao.Author): TaskWithAuthor = {

    ???
  }
}