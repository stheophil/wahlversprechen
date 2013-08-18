package controllers

import play.api._
import play.api.templates._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import com.google.gdata.client.spreadsheet._;
import com.google.gdata.data.spreadsheet._;
import com.google.gdata.util._;
import scala.collection.JavaConversions._;

import models._
import views._

object Application extends Controller with Secured {  
  // Authentification
  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Ungültige E-Mail oder falsches Passwort", result => result match {
      case (email, password) => User.authenticate(email, password).isDefined
    })
  )
  
  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession("email" -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "Du wurdest erfolgreich ausgeloggt"
    )
  }

  // Index page
  private def CountPerResult(l: List[Statement]) : Map[Int, Int] = {
    l.groupBy(_.result.id).map(p => (p._1, p._2.length))
  }
  
  def index = Action { implicit request =>
    val stmts = Statement.allWithoutEntries()
    val catresultmap = stmts.groupBy(_.category.name).map( p => (p._1, CountPerResult(p._2) ) ) + ("" -> CountPerResult(stmts))
    Ok( views.html.index(Statement.allWithoutEntries(), catresultmap, username(request) flatMap { User.load(_) } ) )
  }
  
  // Entry page / entry editing  
  val newEntryForm = Form(
    mapping(
      "content" -> text,
      "stmt_id" -> number,
      "email" -> text
    )((content, stmt_id, email) => Entry(anorm.NotAssigned, anorm.Id(stmt_id), Html(content), new java.util.Date(), User.load(email).get))
     ((entry: Entry) => Some((entry.content.toString(), entry.stmt_id.get.toInt, entry.user.email)))
  )

  private def viewResult(id: Long)(implicit request: Request[AnyContent]) : mvc.Result = { 
    val optstmt = Statement.load(id)
    optstmt match {
      case Some(stmt) => Ok(views.html.detail(stmt, newEntryForm, username(request) flatMap { User.load(_) }))
      case None => NotFound
    }    
  }
  
  def view(id: Long) = Action { implicit request => viewResult(id) }
  
  def addEntry = Action { implicit request =>
    newEntryForm.bindFromRequest.fold(
        formWithErrors => Redirect(routes.Application.index),
        entry => {
          Entry.create(entry.stmt_id.get, entry)
          Redirect(routes.Application.view(entry.stmt_id.get))
        }
    )
  }

  def loadSpreadSheet(spreadsheet: String) = Action { implicit request =>      
    var categorymap = collection.mutable.Map.empty[String, anorm.Pk[Long]]
    var stmtlist = new collection.mutable.ListBuffer[(String, String, String)]
    var count = 0

    try {
      val service = new SpreadsheetService("import");

      // Define the URL to request.  This should never change.
      val WORKSHEET_FEED_URL = new java.net.URL(
          "http://spreadsheets.google.com/feeds/worksheets/" + spreadsheet + "/public/values");

      val worksheet = service.getFeed(WORKSHEET_FEED_URL, classOf[WorksheetFeed]).getEntries().get(0);
      val listFeed = service.getFeed(worksheet.getListFeedUrl(), classOf[ListFeed]);
      
      // Iterate through each row, printing its cell values.
     
      for (row <- listFeed.getEntries()) {
        val custom = row.getCustomElements()
        
        val category = categorymap.getOrElseUpdate(
          custom.getValue("ressort"),
          anorm.NotAssigned
        )
        stmtlist += ( (custom.getValue("titel"), custom.getValue("ressort"), custom.getValue("ergebnis")) )
        count=count+1
      }

      Ok(stmtlist.toString)
    } catch {
      case e: Exception  => Ok(e.toString)
    }     
  }
}


/**
 * Provide security features
 */
trait Secured {  
  def username(request: RequestHeader) = request.session.get("email")
  
  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
  
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

  def IsAdmin(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    val u = User.load(user)
    if(u.isDefined && u.get.role == Role.Admin) {
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }

  def IsEditor(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    val u = User.load(user)
    if(u.isDefined && (u.get.role == Role.Admin || u.get.role==Role.Editor)) { 
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }

}
