package controllers

import javax.inject._
import play.api.mvc._

import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import models.User

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
  def index = Action {
    Ok(views.html.index(LocalDateTime.now(), (1000.0,996.0), "Łukasz"))
  }

}
