package models.api.in

case class Task(author:Int, title: String, description: Option[String], deadline: Option[String])
