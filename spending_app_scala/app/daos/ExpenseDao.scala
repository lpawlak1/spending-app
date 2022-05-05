package daos

import models._
import daos._
import play.api.db.slick._
import services.DateTimeFormatter
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.Future

trait ExpenseDao {
  def getExpensesSum(u_id: Int): Future[Double]

  def findAll: Future[Seq[Expense]]

  def insert(ex: Expense): Future[Int]

  def findWithFilters(u_id: Int, category_id: Option[Int] = None, start_date: Option[String] = None, end_date: Option[String] = None, del: Boolean = false): Future[Seq[Expense]]

  def setDeleted(ex_id: Int, del: Boolean): Future[Int]
}

class ExpenseDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends ExpenseDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  private class ExpenseTable(tag: Tag)
    extends Table[models.Expense](tag, Some("public"), "test1") {
    def expense_id = column[Int]("ex_id", O.PrimaryKey, O.AutoInc)

    def expense_name = column[String]("ex_name")

    def category_id = column[Int]("cat_id")

    def user_id = column[Int]("u_id")

    def added_date = column[LocalDateTime]("addeddatetime")

    def last_mod_date = column[LocalDateTime]("lastmodificationdate")

    def purchase_date = column[LocalDateTime]("dateofpurchase")

    def desc = column[Option[String]]("description")

    def price = column[Double]("price")

    def deleted = column[Boolean]("deleted")

    def category_name = column[Option[String]]("cat_name")

    def * : ProvenShape[Expense] = (
      expense_id.?, expense_name, category_id, user_id, added_date, last_mod_date, purchase_date,
      desc, price, deleted, category_name
      ).mapTo[models.Expense]
  }

  private val expenses_table = TableQuery[ExpenseTable]

  def findAll: Future[Seq[Expense]] = db.run(expenses_table.result)

  def insert(ex: Expense): Future[Int] = {
    val insert = expenses_table += ex
    db.run(insert)
  }

  def setDeleted(ex_id: Int, del: Boolean): Future[Int] = {
    val query = expenses_table.filter(_.expense_id === ex_id).map(_.deleted).update(del)
    db.run(query)
  }

  def findWithFilters(u_id: Int, category_id: Option[Int] = None, start_date: Option[String] = None, end_date: Option[String] = None, del: Boolean = false): Future[Seq[Expense]] = db.run {
    expenses_table
      .filter(_.user_id === u_id)
      .filterOpt(category_id)(_.category_id === _)
      .filterOpt(start_date)(_.purchase_date >= DateTimeFormatter.getDateFromString(_))
      .filterOpt(end_date)(_.purchase_date <= DateTimeFormatter.getDateFromString(_))
      .filterIf(!del)(ex => ex.deleted === false)
      .result
  }

  def getExpensesSum(u_id: Int): Future[Double] = db.run {
    sql"""select public.get_current_users_expenses_sum(${u_id});""".as[Double].head
  }
}
