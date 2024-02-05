package domains.task

import java.time.LocalDateTime

case class Database(id: Option[Int] = None,authorId: Int, title: String, description: Option[String], deadline: Option[LocalDateTime] = None)
