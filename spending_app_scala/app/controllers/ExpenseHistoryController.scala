package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.{ExpenseDao, UserConfigDao}
import models.ThemeColor
import play.api.mvc._
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
class ExpenseHistoryController @Inject()(
                                userConfigDao: UserConfigDao,
                                expenseDao: ExpenseDao,
                                cc: ControllerComponents,
                                @Named("user-authorization-actor") userAuthorizationActor: ActorRef
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val timeout: Timeout = 5.seconds

  def table(user_id: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {

          val themeColorFuture = userConfigDao.getUsersColor(user_id.get.toInt)

          val themeColor = Await.result(themeColorFuture, 3.seconds)
          val retThemeColor = themeColor.getOrElse(ThemeColor.default)

          Ok(views.html.expenses_table()(retThemeColor))
        }
        case false => Redirect(LoginUtils.LOGIN_ERROR_LINK)
      }
    }
  }
}