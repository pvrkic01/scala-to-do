package models.api.out
case class Task(id: Int, title: String, description: Option[String], deadline: Option[String]) extends BaseTaskOut