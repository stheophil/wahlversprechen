package controllers

import play.api._
import play.api.data._
import play.api.templates._
import play.api.mvc._
import play.api.data.Forms._

import com.google.gdata.client.spreadsheet._
import com.google.gdata.data.spreadsheet._
import com.google.gdata.util._
import scala.collection.JavaConversions._

import models._
import views._

object Application extends Controller with Secured {
	// Authentification
	val loginForm = Form(
		tuple(
			"email" -> text,
			"password" -> text) verifying ("Ungültige E-Mail oder falsches Passwort", result => result match {
				case (email, password) => User.authenticate(email, password).isDefined
			}))

	def login = HTTPS { implicit request =>
		Ok(html.login(loginForm))
	}

	def authenticate = Action { implicit request =>
		loginForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.login(formWithErrors)),
			user => Redirect(routes.Application.index).withSession("email" -> user._1))
	}

	def logout = Action {
		Redirect(routes.Application.login).withNewSession.flashing(
			"success" -> "Du wurdest erfolgreich ausgeloggt")
	}

	// Index page
	private def CountPerRating(l: List[Statement]): Map[Int, Int] = {
		l.groupBy(_.rating.id).map(p => (p._1, p._2.length))
	}

	def index = Action { implicit request =>
		val stmts = Statement.allWithoutEntries()
		val catresultmap = stmts.groupBy(_.category.name).map(p => (p._1, CountPerRating(p._2))) + ("" -> CountPerRating(stmts))
		Ok(views.html.index(stmts, catresultmap, username(request) flatMap { User.load(_) }))
	}

	// Entry page / entry editing  
	val newEntryForm = Form(
		tuple(
			"content" -> text.verifying("Kein Text eingegeben", c => !c.isEmpty),
			"stmt_id" -> number.verifying(id => Statement.load(id).isDefined)))

	def view(id: Long) = Action { implicit request =>
		val optstmt = Statement.load(id)
		optstmt match {
			case Some(stmt) => Ok(views.html.detail(stmt, newEntryForm, username(request) flatMap { User.load(_) }))
			case None => NotFound
		}
	}

	def addEntry = IsEditor { user => implicit request =>
		newEntryForm.bindFromRequest.fold(
			formWithErrors => {
				formWithErrors.error("stmt_id") match {
					case Some(e) => Redirect(routes.Application.index).flashing("error" -> "Ungültige Anfrage")
					case None => {
						val stmt_id = formWithErrors("stmt_id").value.get
						val stmt = Statement.load(Integer.parseInt(stmt_id)).get // both Options must be valid if stmt_id verified ok
						Ok(views.html.detail(stmt, formWithErrors, Some(user)))
					}
				}
			},
			t => {
				Entry.create(t._2, t._1, new java.util.Date(), user.id)
				Redirect(routes.Application.view(t._2))
			})
	}

	class ImportException(message: String) extends java.lang.Exception(message)
	
	def loadSpreadSheet(spreadsheet: String) = IsAdmin { user => implicit request =>
		try {
			val service = new SpreadsheetService("import");

			// Define the URL to request.  This should never change.
			val WORKSHEET_FEED_URL = new java.net.URL(
				"http://spreadsheets.google.com/feeds/worksheets/"+spreadsheet+"/public/values");

			val worksheet = service.getFeed(WORKSHEET_FEED_URL, classOf[WorksheetFeed]).getEntries().get(0);
			val listFeed = service.getFeed(worksheet.getListFeedUrl(), classOf[ListFeed]);

			// Iterate through each row, printing its cell values.
			var mapExistingCategories = collection.mutable.Map.empty[String, Category]
			mapExistingCategories ++= (for(c <- Category.loadAll()) yield (c.name, c))
			
			var mapNewCategories = collection.mutable.Map.empty[String, Long] // new category name -> order
			var nCategoryOrder = mapExistingCategories.values.maxBy(_.order).order

			var setTags = collection.mutable.Set.empty[String]

			var nImported = 0
			var cStatements = collection.mutable.ArrayBuffer.empty[(String, String, Option[String], Option[String])] // Titel, Ressort, Zitat, Quelle

			for (row <- listFeed.getEntries()) {
				val custom = row.getCustomElements()

				val strCategory = custom.getValue("ressort")
				if (strCategory == null) throw new ImportException("Fehlendes Ressort bei Wahlversprechen Nr. "+(nImported+1))

				if (!mapExistingCategories.contains(strCategory)) {
					mapNewCategories.getOrElseUpdate(
						strCategory,
						{
							nCategoryOrder = nCategoryOrder + 1
							nCategoryOrder
						}
					)
				}

				val strTitle = custom.getValue("titel")
				if (strTitle == null) throw new ImportException("Fehlender Titel bei Wahlversprechen Nr. "+(nImported+1))

				val strQuote = if (custom.getValue("zitat") == null) None else Some(custom.getValue("zitat"))
				val strSource = if (custom.getValue("quelle") == null) None else Some(custom.getValue("quelle"))
				cStatements += ( (strTitle, strCategory, strQuote, strSource) )

				if (custom.getValue("tags") != null) custom.getValue("tags").split(',').foreach(setTags += _)

				nImported = nImported + 1
			}

			import play.api.Play.current
			play.api.db.DB.withTransaction { c =>
				mapNewCategories.foreach(t => {
					Logger.info("Create category " + t._1 + " with order " + t._2)
					val category = Category.create(c, t._1, t._2)
					mapExistingCategories += (t._1 -> category)
				})

				// TODO insert tags, 
				// TODO quote, source
				cStatements.foreach(t => {
					Logger.info("Create statement " + t._1 + " with category " + t._2)					
					Statement.create(c, t._1, mapExistingCategories.get(t._2).get, Rating.Unrated)
				})
			}

			Redirect(routes.Application.index).flashing("success" -> (nImported+" Wahlversprechen erfolgreich importiert."))
		} catch {
			case e: ImportException => {
				Redirect(routes.Application.index).flashing("error" -> e.getMessage())
			}
			case e: Exception => {
				Logger.error(e.toString)
				Logger.error(e.getStackTraceString)
				Redirect(routes.Application.index).flashing("error" -> "Beim Importieren ist ein Fehler aufgetreten.")
			}
		}
	}
	
	def loader_io = Action {
		Ok("loaderio-1d023df4783a421bb574cfc64791a5c4")
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

	def IsAdmin(f: => User => Request[AnyContent] => Result) = IsAuthenticated { username =>
		request =>
			val u = User.load(username)
			if (u.isDefined && u.get.role == Role.Admin) {
				f(u.get)(request)
			} else {
				Results.Forbidden
			}
	}

	def IsEditor(f: => User => Request[AnyContent] => Result) = IsAuthenticated { username =>
		request =>
			val u = User.load(username)
			if (u.isDefined && (u.get.role == Role.Admin || u.get.role == Role.Editor)) {
				f(u.get)(request)
			} else {
				Results.Forbidden
			}
	}

	/** Called before every request to ensure that HTTPS is used. */
	def HTTPS(f: => Request[AnyContent] => Result) = Action { request =>
		import play.api.Play.current
		if (Play.isDev
			|| request.headers.get("x-forwarded-proto").isDefined
			&& request.headers.get("x-forwarded-proto").get.contains("https")) {
			f(request)
		} else {
			Results.Redirect("https://"+request.host + request.uri);
		}
	}
}
