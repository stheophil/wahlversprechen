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
import models.Rating._
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
	def index = Action { implicit request =>
		val liststmt = Statement.allWithoutEntries().toList.sortBy(_._1.order)
		Ok(views.html.index(liststmt, username(request) flatMap { User.load(_) }))
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
			{ case (content, stmt_id) => {
				Entry.create(stmt_id, content, new java.util.Date(), user.id)
				Redirect(routes.Application.view(stmt_id))
			}}
		)
	}	
	
	val importForm = Form(
		tuple(
			"author" -> text.verifying("Unbekannter Autor", author => Author.load(author).isDefined),
			"spreadsheet" -> text))

	def loadSpreadSheet = IsAdmin { user => implicit request =>
		class ImportException(message: String) extends java.lang.Exception(message)
		case class ImportRow(title: String, category: String, quote: Option[String], quote_source: Option[String], tags: Option[String], merged_id: Option[Long])

		importForm.bindFromRequest.fold(
			formWithErrors => Redirect(routes.Application.index).flashing("error" -> "Ungültige Anfrage"),
			{	case (author_name, spreadsheet) => {

				try {
					val author = Author.load(author_name).get
					val service = new SpreadsheetService("import");
						
					// Define the URL to request.  This should never change.
					val WORKSHEET_FEED_URL = new java.net.URL(
						"http://spreadsheets.google.com/feeds/worksheets/"+spreadsheet+"/public/values");

					val worksheet = service.getFeed(WORKSHEET_FEED_URL, classOf[WorksheetFeed]).getEntries().get(0);
					val listFeed = service.getFeed(worksheet.getListFeedUrl(), classOf[ListFeed]);

					var cRows = collection.mutable.ArrayBuffer.empty[ImportRow]
					for (feedrow <- listFeed.getEntries()) {
						val custom = feedrow.getCustomElements()

						val strCategory = custom.getValue("ressort")
						if (strCategory == null) throw new ImportException("Fehlendes Ressort bei Wahlversprechen Nr. "+(cRows.length+1))

						val strTitle = custom.getValue("titel")
						if (strTitle == null) throw new ImportException("Fehlender Titel bei Wahlversprechen Nr. "+(cRows.length+1))

						val strQuote = if (custom.getValue("zitat") == null) None else Some(custom.getValue("zitat"))
						val strSource = if (custom.getValue("quelle") == null) None else Some(custom.getValue("quelle"))
						val strTags = if (custom.getValue("tags") == null) None else Some(custom.getValue("tags"))
						val merged_id = if (author.rated || custom.getValue("merged") == null) None else {
							try {
								Some( java.lang.Long.parseLong( custom.getValue("merged"), 10 ) )
							} catch {
								case e : NumberFormatException => None
							}
						}
						Logger.info("Found statement " + strTitle)
						cRows += new ImportRow(strTitle, strCategory, strQuote, strSource, strTags, merged_id)
					}

					Logger.info("Found " + cRows.length + " statements. Begin import.")

					import play.api.Play.current
					play.api.db.DB.withTransaction { c =>

						var mapcategory = collection.mutable.Map.empty[String, Category]
						mapcategory ++= (for(c <- Category.loadAll(c)) yield (c.name, c))
						var nCategoryOrder = mapcategory.values.maxBy(_.order).order

						var maptag = collection.mutable.Map.empty[String, Tag]
						maptag ++= (for( t <- Tag.loadAll(c) ) yield (t.name, t))

						cRows.foreach(importrow => {
							Logger.info("Create statement " + importrow.title + " with category " + importrow.category)					

							val category = mapcategory.getOrElseUpdate(
								importrow.category,
								{
									nCategoryOrder = nCategoryOrder + 1
									Logger.info("Create category " + importrow.category + " with order " + nCategoryOrder)
									Category.create(c, importrow.category, nCategoryOrder)
								}
							)

							val stmt = Statement.create(c, importrow.title, author, category, importrow.quote, importrow.quote_source, if(author.rated) Some(Rating.Unrated) else None, importrow.merged_id)
							importrow.tags.foreach( 
									_.split(',').foreach( tagname => {
										val tag = maptag.getOrElseUpdate(tagname, { Tag.create(c, tagname) })
										Tag.add(c, stmt, tag)
									})
							)
						})
					}

					Redirect(routes.Application.index).flashing("success" -> (cRows.length+" Wahlversprechen erfolgreich importiert."))
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
			}}) // bindFromRequest
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
