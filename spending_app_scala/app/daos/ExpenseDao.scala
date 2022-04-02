package daos

import models._
import play.api.db.slick._
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import javax.inject.Inject
import scala.concurrent.Future

trait ExpenseDao {
  def findAll: Future[Seq[Expense]]
  def findExpenseCategory(expense_id: Int): Future[Seq[Expense]]
  def findExpenseSubCategories(expense_id: Int): Future[Seq[Expense]]
  def findDateOfPurchase(expense_id: Int): Future[Seq[Expense]]
  def findDeleted(deleted: Boolean): Future[Seq[Expense]]

  def addExpense(expense_id: Int, expense_name: Option[String], category_id: Option[Int], user_id: Int,
                 added_date: String, last_mod_date: String, purchase_date: String, desc: Option[String],
                 price: Double, deleted: Boolean): Future[Seq[Expense]]
}

class ExpenseDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends ExpenseDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  private class ExpenseTable(tag: Tag)
    extends Table[models.Expense](tag, Some("public"), "expense") {
    def expense_id = column[Int]("ex_id")

    def expense_name = column[Option[String]]("ex_name")

    def category_id = column[Int]("cat_id")

    def user_id = column[Int]("u_id")

    def added_date = column[String]("addedDateTime")

    def last_mod_date = column[String]("lastModificationDate")

    def purchase_date = column[String]("dateOfPurchase")

    def desc = column[Option[String]]("decription")

    def price = column[Double]("price")

    def deleted = column[Boolean]("deleted")

    def * : ProvenShape[Expense] = (
      expense_id, expense_name, category_id, user_id, added_date, last_mod_date, purchase_date,
      desc, price, deleted
      ).mapTo[models.Expense]
  }

  private val table = TableQuery[ExpenseTable]

  def findAll: Future[Seq[Expense]] = db.run(table.result)

  // TODO
}
