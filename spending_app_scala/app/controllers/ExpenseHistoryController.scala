package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.{ExpenseDao, UserConfigDao}
import models.ThemeColor
import play.api.libs.json.Json
import play.api.mvc._
import services.UserAuthorizationActor.UserAuthorization

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

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
          val expensesFuture = expenseDao.findWithFilters(user_id.get.toInt)

          val retThemeColor = Await.result(themeColorFuture, 3.seconds).getOrElse(ThemeColor.default)
          val retExpenses = Await.result(expensesFuture, 1.seconds)



          Ok(views.html.expenses_table(retExpenses)(retThemeColor))
        }
        case false => Redirect(LoginUtils.LOGIN_ERROR_LINK)
      }
    }
  }

  def getRows(user_id: Option[String], category_id: Option[Int], start_date: Option[String], end_date: Option[String], del: Option[Boolean]): Action[AnyContent] = Action.async  {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {
          expenseDao.findWithFilters(user_id.get.toInt, category_id, start_date, end_date, del.getOrElse(false)).map(ret => {
            Ok(Json.toJson(ret.map { x =>
              Map(
                "id" -> x.expense_id.toString,
                "name" -> x.expense_name.toString,
                "category_id" -> x.category_id.toString,
                "user_id" -> x.user_id.toString,
                "added_date" -> x.added_date.toString,
                "last_mod_date" -> x.last_mod_date.toString,
                "purchase_date" -> x.purchase_date.toString,
                "desc" -> x.desc.getOrElse(""),
                "price" -> x.price.toString,
                "deleted" -> x.deleted.toString
              )
            }))
          })
        }
        case false => Future.successful(Unauthorized)
      }.flatten
    }
  }

  def delete(id: Int, del: Option[Boolean]) = Action {
    implicit request => {
      expenseDao.setDeleted(id, del.getOrElse(true))
      Ok("Ok")
    }
  }
}