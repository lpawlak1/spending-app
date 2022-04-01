package daos


import models._
import play.api.db.slick._
import slick.dbio.DBIOAction
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.ProvenShape

import javax.inject.Inject
import scala.concurrent.Future


class UserLoginSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends UserLoginDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  private class UserLoginTable(tag: Tag)
    extends Table[models.LoginUser](tag, Some("public"), "userLogin") {
    def u_id = column[Int]("u_id")

    def u_email = column[String]("u_email")

    def u_password = column[String]("u_password")

    def * : ProvenShape[LoginUser] = (u_id, u_email, u_password).mapTo[models.LoginUser]
  }

  private val table = TableQuery[UserLoginTable]

  def insert(user: models.LoginUser): Future[Unit] = db.run {
    (table += user)
      .andThen(DBIOAction.successful(()))
  }

  implicit val getUserPlainResult2: GetResult[models.LoginUser] = GetResult(r => models.LoginUser(r.<<, r.<<, r.<<))

  def findOneByEmail(email: String): Future[Vector[models.LoginUser]] = db.run {
    sql"""
            select u_id, u_email, u_password
            from public.UserLogin
            where u_email = $email""".as[models.LoginUser]
  }
}

trait UserLoginDao {
  def insert(user: models.LoginUser): Future[Unit]

  def findOneByEmail(email: String): Future[Vector[models.LoginUser]]
}
