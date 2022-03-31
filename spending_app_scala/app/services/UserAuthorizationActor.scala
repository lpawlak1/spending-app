package services

import akka.actor._
import daos.UserDao

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object UserAuthorizationActor {
  def props = Props[UserAuthorizationActor]

  case class UserAuthorization(user_id: Option[String])
}

class UserAuthorizationActor @Inject() (userDao: UserDao) extends Actor{
  import UserAuthorizationActor._
  import context.dispatcher

  def receive: Receive = {
    case UserAuthorization(user_id: Option[String]) => {
      val ret : Future[Boolean] = (if (user_id.isEmpty) None else user_id.get.toIntOption.orElse(None)) match {
        case Some(id) =>
          userDao.findOnesUsername(id).map{
            case usr: Option[String] =>
              if (usr.isEmpty) false
              else true
            case _  => false
          }
        case None => Future(false)
      }
      sender() ! ret
    }
  }
}
