package controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import daos.{CategoryDao, ExpenseDao, TuplesUnpack}
import models.{Category, Expense}
import play.api.data.Form
import play.api.data.Forms.{bigDecimal, mapping, nonEmptyText, number, text}
import play.api.i18n
import play.api.mvc._
import services.UserAuthorizationActor.UserAuthorization

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class ExpenseForInsert(
                             name: String,
                             amount: BigDecimal,
                             purchaseDate: String,
                             category: Int,
                             description: String) {
}



@Singleton
class AddExpenseController  @Inject()(
                                     categoryDAO: CategoryDao,
                                     expenseDAO: ExpenseDao,
                                     cc: ControllerComponents,
                                     @Named("user-authorization-actor") userAuthorizationActor: ActorRef
)(implicit ec: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  implicit val timeout: Timeout = 5.seconds


  object BasicForm {
    val form: Form[ExpenseForInsert] = Form(
      mapping(
        "name" -> nonEmptyText,
        "amount" -> bigDecimal,
        "purchaseDate" -> nonEmptyText,
        "category" -> number,
        "description" -> text
      )(ExpenseForInsert.apply)(ExpenseForInsert.unapply)
    )
  }

  def index(user_id: Option[String]): Action[AnyContent] = Action.async{
    implicit request => {
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

      ret.map(x => {
        Ok(views.html.new_expense(BasicForm.form, user_id.get.toInt, x))
      })

    }
  }

  def post_simple_form(user_id: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    BasicForm.form.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(Redirect("/expense/add?user_id=" + user_id.get))
      },
      expenseForInsert => {
        val purchaseDate = LocalDateTime.parse(expenseForInsert.purchaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        val expense = Expense(expense_name = expenseForInsert.name,
                              category_id = expenseForInsert.category,
                              price = expenseForInsert.amount.toDouble,
                              purchase_date = purchaseDate,
                              desc = if (expenseForInsert.description != "") Some(expenseForInsert.description) else None,
                              deleted = false,
                              user_id = user_id.get.toInt,
                              added_date = LocalDateTime.now(),
                              last_mod_date = LocalDateTime.now())
        expenseDAO.insert(expense).map(x => { Redirect("/?user_id=" + user_id.get) })
      }
    )
  }
}