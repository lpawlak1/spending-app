package models

case class Expense(expense_id: Int, expense_name: Option[String], category_id: Option[Int], user_id: Int,
                   added_date: String, last_mod_date: String, purchase_date: String, desc: Option[String],
                   price: Double, deleted: Boolean) {
  def tupled(tuple: (Int, Option[String], Option[Int], Int, String, String, String, Option[String], Double, Boolean)
            ): Expense = {
    (Expense.apply _).tupled(tuple)
  }
}
