package controllers

import akka.actor.ActorSystem
import controllers.LoginUtils.LOGIN_ERROR_LINK
import daos.UserLoginDao
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import javax.inject._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

object LoginUtils {
  val LOGIN_ERROR_LINK = "/login?err_code=1"
}

case class UserData(email: String, password: String)

@Singleton
class LoginController @Inject()(
                                 cc: ControllerComponents,
                                 actorSystem: ActorSystem,
                                 userLoginDao: UserLoginDao
                               )(implicit exec: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  object BasicForm {
    val form: Form[UserData] = Form(
      mapping(
        "email" -> nonEmptyText,
        "password" -> nonEmptyText
      )(UserData.apply)(UserData.unapply)
    )
  }

  /*
   * After login submit button was clicked this is going to happen
   */
  def post_login_page(): Action[AnyContent] = Action.async { implicit request =>
    val formData: Form[UserData] = BasicForm.form.bindFromRequest()
    if (formData.hasErrors) {
      Future {
        BadRequest(views.html.login(formData))
      }
    }
    else {
      val userData = formData.get
      val email = userData.email
      val password = userData.password
      val user = userLoginDao.findOneByEmail(email)
      user.map {
        _.filter(u => u.password == password).collectFirst(u => u) match {
          case Some(u) =>
            Redirect(s"/?user_id=${u.id}")
          case None =>
            Redirect(LOGIN_ERROR_LINK)
        }
      }
    }
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
