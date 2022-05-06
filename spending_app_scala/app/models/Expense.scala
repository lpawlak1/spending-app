package models

import java.sql.Types
import java.time.LocalDateTime

case class Expense(expense_id: Option[Int] = None, expense_name: String, category_id: Int, user_id: Int,
                   added_date: LocalDateTime, last_mod_date: LocalDateTime, purchase_date: LocalDateTime,
                   desc: Option[String], price: Double, deleted: Boolean, category_name: Option[String] = None) {
  def tupled(tuple: (
    Option[Int], String, Int, Int, LocalDateTime, LocalDateTime, LocalDateTime, Option[String], Double, Boolean, Option[String]
    )): Expense = {
    (Expense.apply _).tupled(tuple)
  }

  def toMap() =  {
  }
}
