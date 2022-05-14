package daos

import akka.event.Logging
import models._
import daos._
import org.postgresql.util.PGmoney
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

  def getSubCategoriesSum(u_id: Int, category_id: Option[Int] = None, start_date: Option[String] = None, end_date: Option[String] = None, del: Boolean = false): Future[Seq[(Option[String], Option[Double])]]

  def getCumulativeDifferences(u_id: Int, category_id: Option[Int] = None, start_date: String, end_date: String): Future[Seq[ExpenseCumulativeResultSet]]
}

case class ExpenseCumulativeResultSet(month: Int, year: Int, difference: Double, sum: Double) {
  def apply(month: Int, year: Int, difference: Double, sum: Double): ExpenseCumulativeResultSet = new ExpenseCumulativeResultSet(month, year, difference, sum)
}

class ExpenseDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends ExpenseDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  private class ExpenseTable(tag: Tag)
    extends Table[models.Expense](tag, Some("public"), "expense_view") {
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

      def category_name = column[Option[String]]("cat_name", O.PrimaryKey, O.AutoInc)
      def superior_cat_id = column[Option[Int]]("cat_superior_cat_id", O.PrimaryKey, O.AutoInc)

      def * : ProvenShape[Expense] = (
        expense_id.?, expense_name, category_id, user_id, added_date, last_mod_date, purchase_date,
        desc, price, deleted, category_name, superior_cat_id
        ).mapTo[models.Expense]
  }

  private val expenses_table = TableQuery[ExpenseTable]

  def findAll: Future[Seq[Expense]] = db.run(expenses_table.result)

  def insert(ex: Expense): Future[Int] = {
    val insert = expenses_table += ex
    db.run(insert)
  }

  def setDeleted(ex_id: Int, del: Boolean): Future[Int] = {
    val query = expenses_table
                  .filter(_.expense_id === ex_id)
                  .map(_.deleted)
                  .update(del)
    db.run(query)
  }

  def findWithFilters(u_id: Int, category_id: Option[Int] = None, start_date: Option[String] = None, end_date: Option[String] = None, del: Boolean = false): Future[Seq[Expense]] = db.run {
    queryFiltering(u_id, category_id, start_date, end_date, del)
      .sortBy(_.purchase_date.asc)
      .result
}

  def getExpensesSum(u_id: Int): Future[Double] = db.run {
    sql"""select public.get_current_users_expenses_sum(${u_id});""".as[Double].head
  }

  private def queryFiltering(u_id: Int, category_id: Option[Int] = None, start_date: Option[String] = None, end_date: Option[String] = None, del: Boolean = false) = {
    expenses_table
      .filter(_.user_id === u_id)
      .filterOpt(category_id)((x,y) => x.category_id === y || x.superior_cat_id === y)
      .filterOpt(start_date)(_.purchase_date >= DateTimeFormatter.getDateFromString(_))
      .filterOpt(end_date)(_.purchase_date <= DateTimeFormatter.getDateFromString(_))
      .filterIf(!del)(ex => ex.deleted === false)
  }

  def getSubCategoriesSum(u_id: Int, category_id: Option[Int] = None, start_date: Option[String] = None, end_date: Option[String] = None, del: Boolean = false): Future[Seq[(Option[String], Option[Double])]] = db.run {
    queryFiltering(u_id, category_id, start_date, end_date, del)
      .groupBy(_.category_name)
      .map {
        case (cat_name, rest) => (cat_name, rest.map(_.price).sum)
      }
      .result
  }


  implicit val getCumulativeResultSet: GetResult[ExpenseCumulativeResultSet] = GetResult(r => ExpenseCumulativeResultSet(r.<<, r.<<, r.<<, r.<<))
  def getCumulativeDifferences(u_id: Int, category_id: Option[Int] = None, start_date: String, end_date: String): Future[Seq[ExpenseCumulativeResultSet]] = db.run {
    println(s"${start_date}\n${end_date}\n${u_id}\n${category_id}\n")
    sql"""
         select * from get_differences_cumulative(${start_date}::timestamp without time zone,
                                                  ${end_date}::timestamp without time zone,
                                                  ${u_id},
                                                  (${category_id})::integer);
       """.as[ExpenseCumulativeResultSet]
  }
}
