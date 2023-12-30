package actors
import akka.actor.{Actor, ActorLogging}

case class Task(id: Int, title: String, description: String = "")

object TaskActor {
  case class GetTasks()
  case class GetTaskById(taskId: Int)
  case class DeleteTaskById(taskId: Int)
  case class AddTask(task: Task)
}

class TaskActor extends Actor with ActorLogging {
  import TaskActor._

  var tasks: List[Task] = List(Task(1, "Do something", "clean housess"), Task(2, "Do something else"))

  override def receive: Receive = {
    case GetTasks =>
      sender() ! tasks
    case GetTaskById(taskId) =>
      sender() ! tasks.find(_.id == taskId)
    case DeleteTaskById(taskId) =>
      tasks = tasks.filter(_.id != taskId)
      sender() ! "Task removed successfully"
    case AddTask(task) =>
      tasks = tasks :+ task
      sender() ! task
  }
}
