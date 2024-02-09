package models.api.out

trait BaseTaskOut {
  def id: Int
  def title: String
  def description: Option[String]
  def deadline: Option[String]
}
