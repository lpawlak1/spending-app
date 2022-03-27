package daos

import models._

import javax.inject.Singleton
import scala.concurrent.Future
import play.api.db.slick._
import slick.dbio.DBIOAction
import slick.jdbc.{GetResult, JdbcProfile}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

trait UserDao {
    def findAll(): Future[Seq[models.User]]
    def insert(user: models.User): Future[Unit]
    def findOne(id: Int): Future[Option[models.UserPlain]]
    def findOneByEmail(email: String): Future[Option[models.User]]
}

//@Singleton
//class UserDaoMap extends UserDao {
//    val map = collection.mutable.Map.empty[Int, models.User]
//
//    def findAll(): Future[Seq[models.User]] = Future.successful(map.values.toSeq)
//    def insert(user: models.User): Future[Unit] = Future.successful(map.update(user.U_ID, user))
//}


class UserDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends UserDao
    with HasDatabaseConfigProvider[JdbcProfile] {

    import dbConfig.profile.api._

    private class UserTable(tag: Tag) 
      extends Table[models.User](tag, Some("public"),"user") {
        def u_id = column[Int]("u_id", O.PrimaryKey)
        def u_name = column[String]("u_name")
        def u_email = column[String]("u_email")
        def u_role = column[String]("u_role")
        def u_password = column[String]("u_password")
        def RegistrationDate = column[LocalDateTime]("registrationdate")

        def * = (u_id, u_name, u_email, u_role, u_password, RegistrationDate).mapTo[models.User]
    }

    private val table = TableQuery[UserTable]

    def findAll(): Future[Seq[models.User]] = db.run(table.result)
    def insert(user: models.User): Future[Unit] = db.run {
        (table += user)
            .andThen(DBIOAction.successful(())) // Return Unit instead of Int
    }

    implicit val getUserPlainResult: GetResult[models.UserPlain] = GetResult(r => models.UserPlain(r.<<, r.<<, r.<<, r.<<, LocalDateTime.parse(r.<<,DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss"))  ))
    def findOne(id: Int): Future[Option[models.UserPlain]] = db.run {
        sql"""
            select u_id, u_name, u_email, u_role, to_char(registrationdate,'YYYY MM DD HH MI SS') as registrationdate
            from public.user
            where u_id = ${id}""".as[models.UserPlain].headOption
    }

    implicit val getUserPlainResult2: GetResult[models.User] = GetResult(r => models.User(r.<<, r.<<, r.<<, r.<<, r.<<, LocalDateTime.parse(r.<<,DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss"))))
    def findOneByEmail(email: String): Future[Option[models.User]] = db.run {
        sql"""
            select u_id, u_name, u_email, u_role, u_password, to_char(registrationdate,'YYYY MM DD HH MI SS') as registrationdate
            from public.user
            where u_email = ${email}""".as[models.User].headOption
    }
}
