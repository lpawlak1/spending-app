package daos

import models._
import play.api.db.slick._
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.Future

trait ExpenseDao {
  def findAll: Future[Seq[Expense]]
  def findExpenseCategory(ex_id: Int): Future[Int]
  def findExpenseSubCategories(expense_id: Int): Future[Int]
  def findDateOfPurchase(start_date: LocalDateTime, end_date: LocalDateTime): Future[Option[LocalDateTime]]
  def findDeleted(del: Boolean): Future[Seq[Expense]]

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

    def category_id = column[Int]("cat_id")

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

  private val table = TableQuery[ExpenseTable]

  def findAll: Future[Seq[Expense]] = db.run(table.result)

  def findDeleted(del: Boolean): Future[Seq[Expense]] = db.run(table.filter(_.deleted === del).result)

  def findExpenseCategory(ex_id: Int): Future[Int] = db.run {
    sqlu"""
          select C.Cat_Name
          from Category C
              join Expense E on E.Cat_ID = C.Cat_ID
        """
  }

  def findExpenseSubCategories(ex_id: Int): Future[Int] = db.run {
    sqlu"""
          select C.Cat_Name
          from Category C
              join Expense E on E.Cat_ID = C.Cat_ID
          where C.Cat_Superior_Cat_Id  = E.Cat_ID
        """
  }

  def findDateOfPurchase(start_date: LocalDateTime, end_date: LocalDateTime): Future[Option[LocalDateTime]] = db.run {
    // TODO:
  }

  def insert(ex: Expense): Future[Int] = db.run {
    sqlu"""
          insert into expense values (${ex.expense_id}, ${ex.expense_name}, ${ex.category_id},
                                      ${ex.user_id}, ${ex.added_date}, ${ex.last_mod_date},
                                      ${ex.purchase_date}, ${ex.desc}, ${ex.price}, ${ex.deleted})
        """
  }
}
