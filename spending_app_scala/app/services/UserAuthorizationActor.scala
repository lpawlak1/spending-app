package services

import akka.actor._
import akka.event.Logging
import daos.UserDao

import javax.inject.Inject
import scala.concurrent.Future

object UserAuthorizationActor {
  def props = Props[UserAuthorizationActor]

  case class UserAuthorization(user_id: Option[String])
}

class UserAuthorizationActor @Inject()(userDao: UserDao) extends Actor {

  import UserAuthorizationActor._
  import context.dispatcher

  val log = Logging(context.system, this)

  override def preStart() = {
    log.error("UserAuthoriztionActor created")
  }
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, "UserAuthorizationActor recreated")
  }

  def receive: Receive = {
    case UserAuthorization(user_id: Option[String]) => {
      val ret: Future[Boolean] = (if (user_id.isEmpty) None else user_id.get.toIntOption.orElse(None)) match {
        case Some(id) =>
          userDao.findOnesUsername(id).map {
            case usr: Option[String] =>
              if (usr.isEmpty) false
              else true
            case _ => false
          }
        case None => Future(false)
      }
      sender() ! ret
    }
  }
}
