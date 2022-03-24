package models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class User(
    U_ID: Int,
    U_Name: String,
    U_Email: String,
    U_Role: String,
    U_Password: String,
    RegistrationDate: LocalDateTime
) {
    def tupled(tuple: (Int, String, String, String, String, LocalDateTime)): User = {
        (User.apply _).tupled(tuple)
    }
}

case class UserPlain(
      U_ID: Int,
      U_Name: String,
      U_Email: String,
      U_Role: String,
      RegistrationDate: LocalDateTime
) {
    def tupled(tuple: (Int, String, String, String, LocalDateTime)): UserPlain = {
        (UserPlain.apply _).tupled(tuple)
    }
}
