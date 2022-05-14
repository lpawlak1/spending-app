package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.{CategoryDao, ExpenseDao, TuplesUnpack, UserConfigDao}
import models.{Category, ThemeColor}
import play.api.libs.json.Json
import play.api.mvc._
import services.DateTimeFormatter
import services.DateTimeFormatter.getDateTime
import services.UserAuthorizationActor.UserAuthorization

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps


@Singleton
class CompareController @Inject()(
                                   userConfigDao: UserConfigDao,
                                   expenseDao: ExpenseDao,
                                   categoryDAO: CategoryDao,
                                   cc: ControllerComponents,
                                   @Named("user-authorization-actor") userAuthorizationActor: ActorRef
                                 )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val timeout: Timeout = 5.seconds

  def compare(user_id: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {
          val zipped = categoryDAO.findTopLevelCategories.map(categories => {
            (categories.foldRight(Future[Any]()) { (category, accumulator) => {
              accumulator.zip(categoryDAO.findSubCategories(category.id))
            }
            },
              categories: Seq[Category])
          })

          val ret = zipped.map(x => {
            val (subCategories, topLevelCategories) = x
            val categories: Future[List[Seq[Any]]] = TuplesUnpack.unpackFuture(ec)(subCategories)

            categories.map(categories => {
              categories.map(category => {
                val subCategories = category.map(x => x.asInstanceOf[Category])

                if (subCategories.nonEmpty) {
                  val topLevelCategory = topLevelCategories.find(x => x.id == subCategories.headOption.get.parent_category_id.get)
                  (topLevelCategory, subCategories)
                }
                else {
                  (None, Seq())
                }
              }).filter(x => x._1.isDefined).map(x => (x._1.get, x._2))
            })
          }).flatten

          val themeColorFuture = userConfigDao.getUsersColor(user_id.get.toInt)
          val retThemeColor = Await.result(themeColorFuture, 3.seconds).getOrElse(ThemeColor.default)

          val categories = Await.result(ret, 1.seconds)
          Ok(views.html.expenses_compare(categories)(retThemeColor))

        }
        case false => Redirect(LoginUtils.LOGIN_ERROR_LINK)
      }
    }
  }

  def getComparedData(user_id: Option[String], category_id: Option[Int], start_date: Option[String], end_date: Option[String]) = Action.async {
    (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
      case true => {
        if (start_date.isEmpty || end_date.isEmpty){
          Future.successful(Unauthorized)
        }
        expenseDao.getCumulativeDifferences(user_id.get.toInt, category_id, DateTimeFormatter.getDateFromString(start_date.get).toString, DateTimeFormatter.getDateFromString(end_date.get).toString).map(f => {
          Ok(Json.toJson(f.map { x =>
            Map(
              "month" -> x.month.toString,
              "year" -> x.year.toString,
              "sum" -> x.sum.toString,
              "difference" -> x.difference.toString
            )
          }))
        })
      }
      case false => Future.successful(Unauthorized)
    }.flatten
  }
}


