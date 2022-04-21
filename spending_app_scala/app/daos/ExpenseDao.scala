package daos

import models._
import daos._
import play.api.db.slick._
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.Future

trait ExpenseDao {
  def findAll: Future[Seq[Expense]]

  def findDeleted(del: Boolean): Future[Seq[(Option[String], LocalDateTime, Option[Int], Double, Option[String])]]

  def findExpenseCategory(ex_id: Int): Future[Int]

  def findExpenseSubCategories(expense_id: Int): Future[Int]

  def findExpensesByPurchaseDate(start_date: LocalDateTime, end_date: LocalDateTime): Future[Seq[
    (Option[String], LocalDateTime, Option[Int], Double, Option[String])]]

  def insert(ex: Expense): Future[Int]
}

class ExpenseDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends ExpenseDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  private class ExpenseTable(tag: Tag)
    extends Table[models.Expense](tag, Some("public"), "expense") {
    def expense_id = column[Int]("ex_id")

    def expense_name = column[Option[String]]("ex_name")

    def category_id = column[Option[Int]]("cat_id")

    def user_id = column[Int]("u_id")

    def added_date = column[LocalDateTime]("addeddatetime")

    def last_mod_date = column[LocalDateTime]("lastmodificationdate")

    def purchase_date = column[LocalDateTime]("dateofpurchase")

    def desc = column[Option[String]]("decription")

    def price = column[Double]("price")

    def deleted = column[Boolean]("deleted")

    def * : ProvenShape[Expense] = (
      expense_id, expense_name, category_id, user_id, added_date, last_mod_date, purchase_date,
      desc, price, deleted
      ).mapTo[models.Expense]
  }

  private val expenses_table = TableQuery[ExpenseTable]

  def findAll: Future[Seq[Expense]] = db.run(expenses_table.result)

  // Lists contents in format: Ex_Name, DateOfPurchase, Cat_ID, Price, Description
  def findDeleted(del: Boolean): Future[Seq[(Option[String], LocalDateTime, Option[Int], Double, Option[String])]] = {
    val query = expenses_table.filter(_.deleted === del).map(ex => (ex.expense_name,
      ex.purchase_date, ex.category_id, ex.price, ex.desc))

    db.run(query.result)
  }

  // Working version of findExpenseCategory. Uses plain sql query.
  def findExpenseCategory(ex_id: Int): Future[Int] = db.run {
    sqlu"""
          select E.Ex_Name,
                 E.DateOfPurchase,
                 C.Cat_Name,
                 E.Price,
                 E.Description
          from Expense E
              join Category C on E.Cat_ID = C.Cat_ID
          where E.Ex_ID = ${ex_id}
        """
  }

  // Working version of findExpenseSubCategories. Uses plain sql query.
  def findExpenseSubCategories(ex_id: Int): Future[Int] = db.run {
    sqlu"""
          select E.Ex_Name,
                 E.DateOfPurchase,
                 C.Cat_Name,
                 E.Price,
                 E.Description
          from Expense E
              join Category C on E.Cat_ID = C.Cat_ID
          where C.Cat_Superior_Cat_Id  = E.Cat_ID
            and E.Ex_ID = ${ex_id}
        """
  }

  // Lists contents in format: Ex_Name, DateOfPurchase, Cat_ID, Price, Description
  def findExpensesByPurchaseDate(start_date: LocalDateTime, end_date: LocalDateTime): Future[Seq[
    (Option[String], LocalDateTime, Option[Int], Double, Option[String])]] = {
    val query = expenses_table.filter(
      ex => (ex.purchase_date >= start_date) && (ex.purchase_date <= end_date)
    ).map(ex => (ex.expense_name, ex.purchase_date, ex.category_id, ex.price, ex.desc))

    db.run(query.result)
  }

  def insert(ex: Expense): Future[Int] = {
    val insert = expenses_table += ex
    db.run(insert)
  }
}