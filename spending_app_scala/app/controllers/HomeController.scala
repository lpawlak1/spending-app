package controllers


import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.{ExpenseDao, UserConfigDao}
import models.ThemeColor
import play.api.mvc._
import services.TuplesUnpack
import services.UserAuthorizationActor.UserAuthorization

import java.time.{Duration, LocalDateTime}
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.math.Fractional.Implicits.infixFractionalOps

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                userDao: daos.UserDao,
                                userConfigDao: UserConfigDao,
                                expenseDao: ExpenseDao,
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
          val themeColorFuture = userConfigDao.getUsersColor(user_id.get.toInt)

          val budget = Await.result(budgetFuture, 2.seconds)
          val retBudget: Double = budget.getOrElse(0)

          val username = Await.result(usernameFuture, 1.seconds)
          val retUsername = username.getOrElse("No username :(")

          val themeColor = Await.result(themeColorFuture, 1.seconds)
          val retThemeColor = themeColor.getOrElse(ThemeColor.default)

          Ok(views.html.index(LocalDateTime.now(), (retBudget, user_id.get.toInt), retUsername, retThemeColor))
        }
        case false => Redirect(LoginUtils.LOGIN_ERROR_LINK)
      }
    }
  }
}
