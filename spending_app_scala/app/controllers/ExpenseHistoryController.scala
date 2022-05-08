package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.{CategoryDao, ExpenseDao, TuplesUnpack, UserConfigDao}
import models.{Category, ThemeColor}
import play.api.libs.json.Json
import play.api.mvc._
import services.DateTimeFormatter.getDateTime
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
                                categoryDAO: CategoryDao,
                                cc: ControllerComponents,
                                @Named("user-authorization-actor") userAuthorizationActor: ActorRef
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val timeout: Timeout = 5.seconds

  def table(user_id: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {
          val zipped = categoryDAO.findTopLevelCategories.map(categories => {
            (categories.foldRight(Future[Any]()){ (category, accumulator) => {
              accumulator.zip(categoryDAO.findSubCategories(category.id))
            }
            },
              categories: Seq[Category])
          })

          val ret = zipped.map(x => {
            val (subCategories,topLevelCategories) = x
            val categories: Future[List[Seq[Any]]] = TuplesUnpack.unpackFuture(ec)(subCategories)

            categories.map(categories => {
              categories.map(category => {
                val subCategories = category.map(x => x.asInstanceOf[Category])

                if (subCategories.nonEmpty){
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
          val expensesFuture = expenseDao.findWithFilters(user_id.get.toInt)

          val retThemeColor = Await.result(themeColorFuture, 3.seconds).getOrElse(ThemeColor.default)
          val retExpenses = Await.result(expensesFuture, 1.seconds)

          val categories = Await.result(ret, 1.seconds)

          Ok(views.html.expenses_table(categories, retExpenses)(retThemeColor))
        }
        case false => Redirect(LoginUtils.LOGIN_ERROR_LINK)
      }
    }
  }

  def chart(user_id: Option[String]): Action[AnyContent] = Action.async {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {
          val zipped = categoryDAO.findTopLevelCategories.map(categories => {
            (categories.foldRight(Future[Any]()){ (category, accumulator) => {
              accumulator.zip(categoryDAO.findSubCategories(category.id))
            }
            },
              categories: Seq[Category])
          })

          val ret = zipped.map(x => {
            val (subCategories,topLevelCategories) = x
            val categories: Future[List[Seq[Any]]] = TuplesUnpack.unpackFuture(ec)(subCategories)

            categories.map(categories => {
              categories.map(category => {
                val subCategories = category.map(x => x.asInstanceOf[Category])

                if (subCategories.nonEmpty){
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
          val expensesFuture = expenseDao.findWithFilters(user_id.get.toInt)

          val retThemeColor = Await.result(themeColorFuture, 3.seconds).getOrElse(ThemeColor.default)
          val retExpenses = Await.result(expensesFuture, 1.seconds)

          val categories = Await.result(ret, 1.seconds)

          Ok(views.html.expenses_chart(categories, retExpenses)(retThemeColor))
        }
        case false => Redirect(LoginUtils.LOGIN_ERROR_LINK)
      }
    }
  }

  def getRows(user_id: Option[String], category_id: Option[Int], start_date: Option[String], end_date: Option[String], del: Option[Boolean]): Action[AnyContent] = Action.async  {
    implicit request => {
      (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
        case true => {
          expenseDao.findWithFilters(user_id.get.toInt, if (category_id.isDefined && category_id.get != -1) category_id else None, start_date, end_date, del.getOrElse(false)).map(ret => {
            Ok(Json.toJson(ret.map { x =>
              Map(
                "id" -> x.expense_id.get.toString,
                "name" -> x.expense_name,
                "category_id" -> x.category_id.toString,
                "category_name" -> x.category_name.getOrElse(""),
                "user_id" -> x.user_id.toString,
                "added_date" -> x.added_date.toString,
                "last_mod_date" -> x.last_mod_date.toString,
                "purchase_date" -> getDateTime(x.purchase_date),
                "desc" -> x.desc.getOrElse(""),
                "price" -> x.price.toString,
                "deleted" -> x.deleted.toString,
                "category" -> x.category_name.getOrElse("")
              )
            }))
          })
        }
        case false => Future.successful(Unauthorized)
      }.flatten
    }
  }

  def getSumPerCategory(user_id: Option[String], category_id: Option[Int], start_date: Option[String], end_date: Option[String], del: Option[Boolean]) = Action.async {
    (userAuthorizationActor ? UserAuthorization(user_id)).mapTo[Future[Boolean]].flatten.map {
      case true => {
        expenseDao.getSubCategoriesSum(user_id.get.toInt, if (category_id.isDefined && category_id.get != -1) category_id else None, start_date, end_date, del.getOrElse(false)).map(f => {
          Ok(Json.toJson(f.filter(ex => ex._1.isDefined && ex._2.isDefined).map { x =>
            Map(
              "category_name" -> x._1.get,
              "price" -> x._2.get.toString,
            )
          }))
        })
      }
      case false => Future.successful(Unauthorized)
    }.flatten
  }

  def delete(id: Int, del: Option[Boolean]) = Action {
    implicit request => {
      expenseDao.setDeleted(id, del.getOrElse(true)) //fire and forget :)
      Ok("Ok")
    }
  }
}