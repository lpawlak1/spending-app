package controllers


import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import play.api.data.Form
import play.api.data.Forms.{bigDecimal, mapping}
import play.api.mvc._
import services.UserAuthorizationActor.UserAuthorization

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class SingleAmount(amount: BigDecimal)

@Singleton
class UserConfigController @Inject()(
                                      @Named("user-authorization-actor") userAuthorizationActor: ActorRef,
                                      cc: ControllerComponents
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
        case true => Ok(views.html.user_config(UserConfigForms.singleAmount))
        case false => Redirect("/login?err_no=2")
      }
    }
  }

  def submit_budget(user_id: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    val budgetData: Form[SingleAmount] = UserConfigForms.singleAmount.bindFromRequest()
    val ret = routes.UserConfigController.config_page(user_id).url
    val addition = if (budgetData.hasErrors) {
      Future {
        "&err_code=1&err_msg="
      }
    }
    else {
      val userData = budgetData.get
      val amount = userData.amount
      // update
      //actor.update_budget(amount, user_id)
      // show
      Future("")
    }
    addition.map(x => Redirect(ret + x))
  }

}

