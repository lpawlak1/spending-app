package controllers

import javax.inject._

import akka.actor.ActorSystem
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.Form

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

case class UserData(email: String, password: String)

@Singleton
class LoginController @Inject()(cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport {
  object BasicForm {
    val form: Form[UserData] = Form(
      mapping(
        "email" -> nonEmptyText,
        "password"  -> nonEmptyText
      )(UserData.apply)(UserData.unapply)
    )
  }
  def post_login_page() = Action { implicit request =>
    val formData: UserData = BasicForm.form.bindFromRequest.get // Careful: BasicForm.form.bindFromRequest returns an Option
    Ok(formData.toString) // just returning the data because it's an example :)
  }

  /**
   * Creates an Action that returns a plain text message after a delay
   * of 1 second.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/message`.
   */
  def message = Action.async {
    getFutureMessage(1.second).map { msg => Ok(msg) }
  }

  def get_login_page = Action { implicit request =>
    Ok(views.html.login(BasicForm.form))
  }

  private def getFutureMessage(delayTime: FiniteDuration): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    actorSystem.scheduler.scheduleOnce(delayTime) {
      promise.success("Hi!")
    }(actorSystem.dispatcher) // run scheduled tasks using the actor system's dispatcher
    promise.future
  }

}
