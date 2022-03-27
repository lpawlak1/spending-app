package controllers

import models.{UserMinimal }

import javax.inject._
import play.api.mvc._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.language.postfixOps
import java.time.LocalDateTime

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
  userDao: daos.UserDao,
  cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get = {
    userDao.findAll()
  }

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(user_id: Option[String]): Action[AnyContent] = Action.async{
    implicit request => {
      /*Tutaj sprawdzam czy użytkownik podany w query parameter istnieje itp*/
      (if (user_id.isEmpty) None else user_id.get.toIntOption.orElse(None)) match {
        case Some(id) =>
          userDao.findOnesUsername(id).map{
            case usr: Option[String] =>
              if (usr.isEmpty) Redirect("/login")
              else Ok(views.html.index(LocalDateTime.now(), (1000.0,id), usr.get))
            case _  => Redirect(LoginUtils.LOGIN_ERROR_LINK)
          }
        case None => Future(Redirect(LoginUtils.LOGIN_ERROR_LINK))
      }
    }
  }
}
