package controllers

import models.UserPlain

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
  def index(user_id: Option[String]) = Action.async{
    implicit request => {
      if (user_id.isEmpty){
        Future(Redirect("/login"))
      }
      else{
        val id = if (user_id.isEmpty) -1 else user_id.get.toInt
        val usr_plain = userDao.findOne(id)
        usr_plain.map{
          case usr: Option[UserPlain] => Ok(views.html.index(LocalDateTime.now(), (1000.0,id), usr.get.U_Name)) // dodac obsluge braku osoby (TODO)
          case _  => Redirect("/login")
        }
      }
    }
  }

}
