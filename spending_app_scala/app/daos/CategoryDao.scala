package daos

import models._
import play.api.db.slick._
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CategoryDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends CategoryDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  private class CategoryTable(tag: Tag)
    extends Table[models.Category](tag, Some("public"), "category") {

    def cat_id = column[Int]("cat_id")

    def cat_name = column[String]("cat_name")

    def parent_id = column[Option[Int]]("cat_superior_cat_id")

    def * : ProvenShape[Category] = (cat_id, cat_name, parent_id).mapTo[models.Category]
  }

  private val category_table = TableQuery[CategoryTable]

  def findAll: Future[Seq[Category]] = db.run(category_table.result)

  def findTopLevelCategories: Future[Seq[Category]] = db.run(category_table.filter(_.parent_id.isEmpty).result)

  def findSubCategories(parentId: Int): Future[Seq[Category]] = db.run(category_table.filter(_.parent_id === parentId).result)

}



trait CategoryDao {
  def findAll: Future[Seq[Category]]

  def findTopLevelCategories: Future[Seq[Category]]

  def findSubCategories(parentId: Int): Future[Seq[Category]]
}

object TuplesUnpack {
  def unpackFuture(ex: ExecutionContext)(tuple: Future[Any]): Future[List[Seq[Any]]] = {
    tuple.map {
      x => {
        unpack_rec(x)
      }
    }(ex)
  }

  def unpack(tuple: (Any, Seq[Any])): List[Seq[Any]] = {
    unpack_rec(tuple)
  }

  private def unpack_rec(tuple2: Any): List[Seq[Any]] = {
    tuple2 match {
      case (x: (Any, Seq[Any]), y: Seq[Any]) => unpack_rec(x) ::: List(y)
      case (x: Seq[Any], y: Seq[Any]) => List(x, y)
      case ((), y: Seq[Any]) => List(y)
      case x => throw new Exception("unpack_rec: unexpected not tuple" + x)
    }
  }
}
