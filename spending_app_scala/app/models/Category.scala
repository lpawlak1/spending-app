package models

case class Category(id: Int, name: String, parent_category_id: Option[Int]){
  def tupled(tuple: (Int, String, Option[Int])): Category = {
    (Category.apply _).tupled(tuple)
  }
}
