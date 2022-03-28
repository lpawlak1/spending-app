package daos

import models._

import scala.concurrent.Future
import play.api.db.slick._
import slick.dbio.DBIOAction
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.ProvenShape

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

trait UserDao {
    def findOnesUsername(id: Int): Future[Option[String]]
    def findAll(): Future[Seq[models.User]]
    def insert(user: models.User): Future[Unit]
    def findOne(id: Int): Future[Option[models.UserMinimal]]
    def findOneByEmail(email: String): Future[Option[models.User]]
}

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
        def u_password = column[Option[String]]("u_password")
        def RegistrationDate = column[LocalDateTime]("registrationdate")

        def * : ProvenShape[User] = (u_id, u_name, u_email, u_role, u_password, RegistrationDate).mapTo[models.User]
    }

    private val table = TableQuery[UserTable]

    def findAll(): Future[Seq[models.User]] = db.run(table.result)
    def insert(user: models.User): Future[Unit] = db.run {
        (table += user)
            .andThen(DBIOAction.successful(()))
    }

    implicit val getUserPlainResult: GetResult[models.UserMinimal] = GetResult(r => models.UserMinimal(r.<<, r.<<, r.<<, r.<<))
    def findOne(id: Int): Future[Option[models.UserMinimal]] = db.run {
        sql"""
            select u_id, u_name, u_email, u_role
            from public.user
            where u_id = ${id}""".as[models.UserMinimal].headOption
    }

    implicit val getUserPlainResult2: GetResult[models.User] = GetResult(r => models.User(r.<<, r.<<, r.<<, r.<<, r.<<, LocalDateTime.parse(r.<<,DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss"))))
    def findOneByEmail(email: String): Future[Option[models.User]] = db.run {
        sql"""
            select u_id, u_name, u_email, u_role, u_password, to_char(registrationdate,'YYYY MM DD HH MI SS') as registrationdate
            from public.user
            where u_email = ${email}""".as[models.User].headOption
    }

    override def findOnesUsername(id: Int): Future[Option[String]] = db.run(table.filter(_.u_id === id).map(_.u_name).result.headOption)
}
