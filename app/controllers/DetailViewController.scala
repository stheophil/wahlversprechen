package controllers

import models._
import models.Rating._

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.Logger

import views._

object DetailViewController extends Controller with Secured {
	// Entry page / entry editing  

	def view(id: Long) = CachedAction("view."+id) { implicit request =>
		val liststmt = Statement.loadAll(id)
		liststmt.find(_.id == id) match {
			case Some(stmt) => 
				Ok(views.html.detail(
					Statement.loadEntriesTags(stmt), 
					liststmt.map(_.author).distinct, 
					user(request)
				))
			case None => 
				NotFound
		}
	}

	def viewAsFeed(id: Long) = CachedAction("viewAsFeed."+id, 60 * 60) { implicit request =>
		Statement.load(id) match {
			case Some(stmt) => 
					Ok(views.xml.entryList(
					"Wahlversprechen 2013: " + stmt.title, 
					routes.DetailViewController.view(id).absoluteURL(false),
					Entry.loadByStatement(id).take(10)
				))		
			case None => NotFound
		}		
	}

	val newEntryForm = Form("content" -> nonEmptyText)

	def addEntry(stmt_id: Long) = IsEditor { user => implicit request =>
		newEntryForm.bindFromRequest.fold(
			formWithErrors => BadRequest(""),
			{ case (content) => {
				Entry.create(stmt_id, content, new java.util.Date(), user.id)
				Ok("")
			}}
		)
	}	

	val updateItemForm = Form(
		tuple(
			"title" -> optional(nonEmptyText),
			"rating" -> optional(number),
			"quote" -> optional(text),
			"quote_src" -> optional(text),
			"tags" -> optional(text),
			"merged_id" -> optional(number).verifying(id => 
				if(id.isDefined) {
					val stmt = Statement.load(id.get)
					stmt.isDefined && stmt.get.author.rated
				} else {
					true
				}
			) 
	))

	def update(stmt_id: Long) = IsEditor { user => implicit request =>
		updateItemForm.bindFromRequest.fold(
			formWithErrors => BadRequest(""),
			{ case (title, rating, quote, quote_src, tags, merged_id) => {
				Logger.info("Update item " + stmt_id + " (" + title + ", " + rating + ", " + quote + ", " + quote_src + ")" )
				if(title.isDefined) Statement.setTitle(stmt_id, title.get)
				if(rating.isDefined) Statement.setRating(stmt_id, rating.get, new java.util.Date())
				if(quote.isDefined) Statement.setQuote(stmt_id, quote.get)
				if(quote_src.isDefined) Statement.setQuoteSrc(stmt_id, quote_src.get)

				Ok("")
			}}
		)
	}

	def getEntry(entry_id: Long) = Action { implicit request =>
		Entry.contentAsMarkdown(entry_id) match {
			case Some(content) => Ok(content)
			case None => NotFound
		}
	}
}