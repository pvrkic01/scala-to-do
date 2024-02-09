package models.dao

import java.time.LocalDateTime

case class Task(id: Option[Int] = None, authorId: Int, title: String, description: Option[String], deadline: Option[LocalDateTime] = None)
