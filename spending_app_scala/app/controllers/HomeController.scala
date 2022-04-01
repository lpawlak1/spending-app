package controllers


import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import play.api.mvc._
import services.UserAuthorizationActor.UserAuthorization

import java.time.LocalDateTime
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                userDao: daos.UserDao,
                                cc: ControllerComponents,
                                @Named("user-authorization-actor") userAuthorizationActor: ActorRef
  )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val timeout: Timeout = 5.seconds

  def index(user_id: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true =>
          userDao.findOnesUsername(user_id.get.toInt).map {
            case usr: Option[String] =>
              Ok(views.html.index(LocalDateTime.now(), (1000.0, user_id.get.toInt), usr.get))
            case _ => Redirect(LoginUtils.LOGIN_ERROR_LINK)
          }
        case false => Future(Redirect(LoginUtils.LOGIN_ERROR_LINK))
      }.flatten
    }
  }
}
