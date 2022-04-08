package controllers


import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.UserConfigDao
import play.api.mvc._
import services.UserAuthorizationActor.UserAuthorization

import java.time.{Duration, LocalDateTime}
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                userDao: daos.UserDao,
                                userConfigDao: UserConfigDao,
                                cc: ControllerComponents,
                                @Named("user-authorization-actor") userAuthorizationActor: ActorRef
  )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val timeout: Timeout = 5.seconds

  def index(user_id: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {

          val budgetFuture = userConfigDao.getCurrentActiveBudget(user_id.get.toInt)
          val usernameFuture = userDao.findOnesUsername(user_id.get.toInt)

          val res = budgetFuture.zip(usernameFuture)
          res.map {
            case (Some(budget), Some(username)) =>
              Ok(views.html.index(LocalDateTime.now(), (budget, user_id.get.toInt), username))
            case (_, Some(username)) =>
              Ok(username + " has no active budget")
            case (_,_) =>
              Redirect(LoginUtils.LOGIN_ERROR_LINK)
          }

        }
        case false => Future(Redirect(LoginUtils.LOGIN_ERROR_LINK))
      }.flatten
    }
  }
}
