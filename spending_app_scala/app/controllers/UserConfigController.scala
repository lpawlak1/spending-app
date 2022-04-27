package controllers


import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.UserConfigDao
import models.ThemeColor
import play.api.data.Form
import play.api.data.Forms.{bigDecimal, mapping}
import play.api.mvc._
import services.UserAuthorizationActor.UserAuthorization

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

case class SingleAmount(amount: BigDecimal)

@Singleton
class UserConfigController @Inject()(
                                      @Named("user-authorization-actor") userAuthorizationActor: ActorRef,
                                      cc: ControllerComponents,
                                      userConfigDao: UserConfigDao
                                    )(implicit ec: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  implicit val timeout: Timeout = 5.seconds

  object UserConfigForms {
    val singleAmount: Form[SingleAmount] = Form(
      mapping(
        "amount" -> bigDecimal,
      )(SingleAmount.apply)(SingleAmount.unapply)
    )
  }

  def config_page(user_id: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {
          val budgetF =  userConfigDao.getCurrentActiveBudget(user_id.get.toInt)
          val userColorF = userConfigDao.getUsersColor(user_id.get.toInt)
          val colorsF = userConfigDao.getAllColors

          val budget: Double = Await.result(budgetF, 2.seconds).getOrElse(0)
          val userColor = Await.result(userColorF, 1.seconds).getOrElse(ThemeColor.default)
          val colors = Await.result(colorsF, 1.seconds)

          Ok(views.html.user_config(UserConfigForms.singleAmount.fill(SingleAmount(budget)),UserConfigForms.singleAmount, colors, user_id, userColor))
        }
        case false => Redirect("/login?err_no=2")
      }
    }
  }

  def submit_budget(user_id: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
      case true => {
        val budgetData: Form[SingleAmount] = UserConfigForms.singleAmount.bindFromRequest()
        val ret = routes.UserConfigController.config_page(user_id).url
        val addition = if (budgetData.hasErrors) {
          Future {
            "&err_code=1&err_msg="
          }
        }
        else {
          if (user_id.isEmpty) {
            Future {
              "&err_code=1&err_msg="
            }
          }
          val userData = budgetData.get
          val amount = userData.amount
          userConfigDao.insertBudget(user_id.get.toInt, amount.toDouble).map(x =>
            s"&success=${x.toInt}")
        }
        addition.map(x => Redirect(ret + x))
      }
      case false => {
        Future.successful(Redirect("/login"))
      }
    }.flatten
  }

  def change_color(user_id: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
      case true => {
        val colorData: Form[SingleAmount] = UserConfigForms.singleAmount.bindFromRequest()
        val ret = routes.UserConfigController.config_page(user_id).url
        val addition = if (colorData.hasErrors) {
          Future {
            "&err_code=1&err_msg="
          }
        }
        else {
          if (user_id.isEmpty) {
            Future {
              "&err_code=1&err_msg="
            }
          }
          val color = colorData.get.amount.toInt
          userConfigDao.changeColor(user_id.get.toInt, color).map(x =>
            s"&success=${x.toInt}")
        }
        addition.map(x => Redirect(ret + x))
      }
      case false => {
        Future.successful(Redirect("/login"))
      }
    }.flatten
  }

}

