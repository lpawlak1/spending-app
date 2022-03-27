package models

import java.time.LocalDateTime

case class UserMinimal(id: Int,
                       name: String,
                       email: String,
                       role: String) {
    def tupled(tuple: (Int, String, String, String)): UserMinimal = {
        (UserMinimal.apply _).tupled(tuple)
    }
}

case class User(id: Int,
                name: String,
                email: String,
                role: String,
                password: Option[String],
                registrationDate: LocalDateTime) {
    def tupled(tuple: ( Int, String, String, String, Option[String], LocalDateTime )): User = {
        (User.apply _).tupled(tuple)
    }
}
