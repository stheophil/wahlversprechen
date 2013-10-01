package controllers

import play.api._
import play.api.cache._
import play.api.data._
import play.api.templates._
import play.api.mvc._
import play.api.data.Forms._
import play.api.Play.current

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
		val optuser = user(request)

		val statistics = ( Author.loadRated match {
			case Some(author) => Statement.countRatings(author)
			case None => (1, Map.empty[Rating, Int])
		})

		// TODO: Use first author again
		// TODO: Define important tags in database
		Ok(views.html.index(statistics._1, statistics._2, Statement.byEntryDate(None, Some(5)), Statement.byTag("10-Punkteprogramm", None, Some(5)), user(request)))
	}
	
	def recent = Action { implicit request => 		
		val mapstmtByAuthor = Statement.byEntryDate(None, None).groupBy(_.author)
		Ok(views.html.list("Alle Wahlversprechen nach letzter Aktualisierung", mapstmtByAuthor, user(request)))
	}

	def top = Action { implicit request =>  
		// TODO: Define important tags in database
		Ok(views.html.listByCategory("Die wichtigsten Wahlversprechen nach Ressorts", Statement.byTag("10-Punkteprogramm", None, None).groupBy(_.author), user(request)))
	}

	def all = Action { implicit request => 
		Ok(views.html.listByCategory("Alle Wahlversprechen nach Ressorts", Statement.all(), user(request)))
	}

	def tag(tag: String) = Action { implicit request =>
		Ok(views.html.list("Wahlversprechen mit Tag '"+tag+"'", Statement.byTag(tag, None, None).groupBy(_.author), user(request)))		
	}

	def category(category: String) = Action { implicit request => 
		Ok(views.html.list("Wahlversprechen aus dem Ressort '"+category+"'", Statement.byCategory(category, None, None).groupBy(_.author), user(request)))		
	}

	def rating(ratingId: Int) = Action { implicit request => 
		if(0<= ratingId && ratingId < Rating.maxId ) {
			val rating = Rating(ratingId)
			Ok(views.html.listByCategory("Wahlversprechen mit Bewertung '"+Formatter.name(rating)+"'", Statement.byRating(rating, None, None).groupBy(_.author), user(request)))		
		} else {
			NotFound
		}
	}

	def search(query: String) = Action { implicit request => 
		val mapstmtByAuthor = Statement.find(query)
		Ok(views.html.listByCategory("Suchergebnisse", mapstmtByAuthor, user(request) ))
	}	
	
	def loader_io = Action {
		Ok("loaderio-a8c18c9612671703651ea5e79d55623e")
	}
}

/**
 * Provide security features
 */
trait Secured {
	def username(request: RequestHeader) = request.session.get("email")
	def user(request: RequestHeader) = username(request) flatMap { User.load(_) }

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
