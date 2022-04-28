package models

import java.sql.Types
import java.time.LocalDateTime

case class Expense(expense_id: Int, expense_name: String, category_id: Int, user_id: Int,
                   added_date: LocalDateTime, last_mod_date: LocalDateTime, purchase_date: LocalDateTime,
                   desc: Option[String], price: Double, deleted: Boolean, category_name: String) {
  def tupled(tuple: (
    Int, String, Int, Int, LocalDateTime, LocalDateTime, LocalDateTime, Option[String], Double, Boolean, String
    )): Expense = {
    (Expense.apply _).tupled(tuple)
  }
}
