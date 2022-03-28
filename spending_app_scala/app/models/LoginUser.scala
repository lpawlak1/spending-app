package models

case class LoginUser(id:Int, email: String, password: String){
  def tupled(tuple: ( Int, String, String)): LoginUser = {
    (LoginUser.apply _).tupled(tuple)
  }
}
