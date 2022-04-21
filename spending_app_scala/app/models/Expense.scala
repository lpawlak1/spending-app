package models

import java.sql.Types
import java.time.LocalDateTime

case class Expense(expense_id: Int, expense_name: Option[String], category_id: Option[Int], user_id: Int,
                   added_date: LocalDateTime, last_mod_date: LocalDateTime, purchase_date: LocalDateTime,
                   desc: Option[String], price: Double, deleted: Boolean) {
  def tupled(tuple: (
    Int, Option[String], Option[Int], Int, LocalDateTime, LocalDateTime, LocalDateTime, Option[String], Double, Boolean
    )): Expense = {
    (Expense.apply _).tupled(tuple)
  }
}
