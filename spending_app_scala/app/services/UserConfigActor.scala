package services

import akka.actor._
import akka.event.{Logging, LoggingAdapter}
import daos.{UserConfigDao, UserDao}
import models.ThemeColor

import javax.inject.Inject
import scala.concurrent.Future

object UserConfigActor {
  def props = Props[UserConfigActor]

  case class UserAuthorization(user_id: Option[String])
  // BUDŻET
  //Budget na aktualny miesiąc
  case class CurrentBudget(user_id: Int)
  //Insert do Bazy nowego budżetu
  case class CurrentBudgetInsert(user_id: Int, new_budget: Double)
//  def getCurrentActiveBudget(user_id: Int): Future[Option[Double]]
//  def insertBudget(user_id: Int, new_budget: Double): Future[Int]

  // KOLORY
  // zmien kolor dla danego użytkownika
//  def changeColor(user_id: Int, color_id: Int): Future[Int]
  case class ColorInsert(user_id: Int, color_id: Int)
  // pobierz wszystkie kolory
//  def getAllColors: Future[Seq[ThemeColor]]
  case class Colors()
  // pobierz kolor który ma dany użytkownik
//  def getUsersColor(user_id: Int): Future[Option[ThemeColor]]
  case class UserColor(user_id: Int)

}

class UserConfigActor @Inject()(userConfigDao: UserConfigDao) extends Actor {

  import UserConfigActor._
  import context.dispatcher

  val log: LoggingAdapter = Logging(context.system, this)

  override def preStart(): Unit = {
    log.error("UserConfigActor created")
  }
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, "UserConfigActor recreated")
  }

  def receive: Receive = {
    case Colors() => {
      sender() ! userConfigDao.getAllColors
    }
  }
}

