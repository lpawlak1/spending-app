package daos

import models._

import scala.concurrent.Future
import play.api.db.slick._
import slick.dbio.DBIOAction
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.ProvenShape

import javax.inject.Inject

class CategoryDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends CategoryDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  private class CategoryTable(tag: Tag)
    extends Table[models.Category](tag, Some("public"),"category") {
    def cat_id = column[Int]("cat_id")
    def cat_name = column[String]("cat_name")
    def parent_id = column[Option[Int]]("cat_superior_cat_id")

    def * : ProvenShape[Category] = (cat_id, cat_name, parent_id).mapTo[models.Category]
  }

  private val table = TableQuery[CategoryTable]

  def findAll: Future[Seq[Category]] = db.run(table.result)
  def findTopLevelCategories: Future[Seq[Category]] = db.run(table.filter(_.parent_id.isEmpty).result)
  def findSubCategories(parentId: Int): Future[Seq[Category]] = db.run(table.filter(_.parent_id === parentId).result)

}

trait CategoryDao {
  def findAll: Future[Seq[Category]]
  def findTopLevelCategories: Future[Seq[Category]]
  def findSubCategories(parentId: Int): Future[Seq[Category]]
}
