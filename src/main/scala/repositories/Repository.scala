package repositories

import scala.concurrent.Future

trait Repository[T,ID] {

    // add or update
    def save(entity: T): Future[Int]

    def deleteById(id: ID): Future[Int]

    def findById(id: ID): Future[Option[T]]

    def findAll(): Future[List[T]]


}
