package controllers

import models._
import models.Rating._

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

import views._

object DetailViewController extends Controller with Secured {
	// Entry page / entry editing  
	val newEntryForm = Form(
		tuple(
			"content" -> text.verifying("Kein Text eingegeben", c => !c.isEmpty),
			"stmt_id" -> number.verifying(id => Statement.load(id).isDefined)))

	def view(id: Long) = Action { implicit request =>
		internalView(id, newEntryForm, user(request))
	}

	private def internalView(id: Long, form: Form[(String, Int)], user: Option[User])(implicit request: Request[AnyContent]) : play.api.mvc.Result = { 
		val liststmt = Statement.loadAll(id)
		liststmt.find(_.id == id) match {
			case Some(stmt) => 
				Ok(views.html.detail(
					Statement.loadEntriesTags(stmt), 
					liststmt.map(_.author).distinct, 
					form, 
					user
				))
			case None => 
				NotFound
		}
	}

	def addEntry = IsEditor { user => implicit request =>
		newEntryForm.bindFromRequest.fold(
			formWithErrors => {
				formWithErrors.error("stmt_id") match {
					case Some(e) => Redirect(routes.Application.index).flashing("error" -> "Ungültige Anfrage")
					case None => {
						val stmt_id = formWithErrors("stmt_id").value.get
						internalView(Integer.parseInt(stmt_id), formWithErrors, Some(user))
					}
				}
			},
			{ case (content, stmt_id) => {
				Entry.create(stmt_id, content, new java.util.Date(), user.id)
				Redirect(routes.DetailViewController.view(stmt_id))
			}}
		)
	}	
}