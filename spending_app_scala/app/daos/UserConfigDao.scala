package daos

import models._
import play.api.db.slick._
import slick.jdbc.{GetResult, JdbcProfile}

import javax.inject.Inject
import scala.concurrent.Future


class UserConfigDaoSlick @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends UserConfigDao
    with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  def getCurrentActiveBudget(user_id: Int): Future[Option[Double]] = db.run {
    sql"""
         select b_amount::NUMERIC
         from public.budget
         where u_id = ${user_id}
           and b_active = true
           and to_date(to_char(now(), 'YYYY-MM'), 'YYYY-MM') = b_starting_date;
       """.as[Double].headOption
  }

  def insertBudget(user_id: Int, new_budget: Double): Future[Int] = db.run {
    sqlu"""
        call insert_budget($user_id, (${new_budget})::NUMERIC::money);
        """
  }

  // zmien kolor dla danego użytkownika
  def changeColor(user_id: Int, color_id: Int): Future[Int] = db.run {
    sqlu"""
         update public.user
         set Col_ID = $color_id
         where public.user.u_id = $user_id;
       """
  }

  implicit val getColors: GetResult[ThemeColor] = GetResult(r => ThemeColor(r.<<, r.<<, r.<<))
  // pobierz wszystkie kolory
  def getAllColors: Future[Seq[ThemeColor]] = db.run {
    sql"""
         select * from public.colors;
       """.as[ThemeColor]
  }
  // pobierz kolor który ma dany użytkownik
  def getUsersColor(user_id: Int): Future[Option[ThemeColor]] = db.run {
    sql"""
        select c.col_id, c.name, c.col_filename
        from public.user
        inner join colors c on c.col_id = "user".col_id
        where u_id = $user_id;
       """.as[ThemeColor].headOption
  }

}

trait UserConfigDao {
  // BUDŻET
  //Budget na aktualny miesiąc
  def getCurrentActiveBudget(user_id: Int): Future[Option[Double]]
  //Insert do Bazy nowego budżetu
  def insertBudget(user_id: Int, new_budget: Double): Future[Int]

  // KOLORY
  // zmien kolor dla danego użytkownika
  def changeColor(user_id: Int, color_id: Int): Future[Int]
  // pobierz wszystkie kolory
  def getAllColors: Future[Seq[ThemeColor]]
  // pobierz kolor który ma dany użytkownik
  def getUsersColor(user_id: Int): Future[Option[ThemeColor]]

}
