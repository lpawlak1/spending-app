package controllers


import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import play.api.data.{Form, Mapping}
import play.api.data.Forms.{bigDecimal, date, mapping, nonEmptyText, of, optional}

import javax.inject._
import play.api.mvc._
import services.UserAuthorizationActor.UserAuthorization

import java.util.Date
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

case class Budget(amount: BigDecimal, startingDate: Option[Date])

@Singleton
class UserConfigController @Inject()(
                                      @Named("user-authorization-actor") userAuthorizationActor: ActorRef,
                                      cc: ControllerComponents
                                   )(implicit ec: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  implicit val timeout: Timeout = 5.seconds

  object UserConfigForms {
    val budgetForm: Form[Budget] = Form(
      mapping(
        "amount" -> bigDecimal(2,1),
        "startingDate" -> optional(date("mm/dd/yyyy"))
      )(Budget.apply)(Budget.unapply)
    )
  }

  def config_page(user_id: Option[String]): Action[AnyContent] = Action.async{
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map{
        case true => Ok(views.html.user_config(UserConfigForms.budgetForm))
        case false => Redirect("/login?err_no=2")
      }
    }
  }
}

