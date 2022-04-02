package daos

import models._
import org.postgresql.util.PGmoney
import play.api.db.slick._
import slick.jdbc.JdbcProfile

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

}

trait UserConfigDao {
  //Budget na aktualny miesiąc
  def getCurrentActiveBudget(user_id: Int): Future[Option[Double]]
  //TODO insert do budget przez procedure
  def insertBudget(user_id: Int, new_budget: Double): Future[Int]
}
