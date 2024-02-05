package domains.task

import domains.author.{Database => AuthorImplementation}

trait BaseTaskOut {
  def id: Int
  def title: String
  def description: Option[String]
  def deadline: Option[String]
}
case class Out(id: Int, title: String, description: Option[String], deadline: Option[String]) extends BaseTaskOut
case class OutWithAuthor(id: Int, title: String, description: Option[String], deadline: Option[String],author: AuthorImplementation) extends BaseTaskOut

object TaskWithAuthor {
  // Implicitna klasa koja dodaje autora na Out i stvara TaskWithAuthor
  implicit class WithAuthor(out: Out) {
    def withAuthor(author: AuthorImplementation): OutWithAuthor = {
      OutWithAuthor(out.id, out.title, out.description, out.deadline, author)
    }
  }
}
