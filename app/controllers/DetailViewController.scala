package controllers

import models._
import models.Rating._

import play.api.cache._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.Logger
import play.api.Play.current

import views._

object DetailViewController extends Controller with Secured {
	// Entry page / entry editing  

	def view(id: Long) = CachedAction("view."+id) { implicit request =>		
		Statement.load(id) match {
			case Some(stmt) => 
				Ok(views.html.detail(
					Statement.withEntries(stmt), 
					if(stmt.author.rated) {
						Statement.loadAll(id)
					} else if(stmt.merged_id.isDefined) {
						Statement.loadAll(stmt.merged_id.get)
					} else {
						List(stmt)
					}, 
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
				Cache.remove("view."+stmt_id)
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
				Logger.debug("Update item " + stmt_id + " (" + title + ", " + rating + ", " + quote + ", " + quote_src + ")" )
				if(title.isDefined) Statement.setTitle(stmt_id, title.get)
				if(rating.isDefined) Statement.setRating(stmt_id, rating.get, new java.util.Date())
				if(quote.isDefined) Statement.setQuote(stmt_id, quote.get)
				if(quote_src.isDefined) Statement.setQuoteSrc(stmt_id, quote_src.get)
				Cache.remove("view."+stmt_id)

				Ok("")
			}}
		)
	}

	def delete(stmt_id: Long) = IsAdmin { user => implicit request =>
		Statement.delete(stmt_id)
		Cache.remove("view."+stmt_id)
		Ok("")
	}

	def getEntry(entry_id: Long) = Action { implicit request =>
		Entry.contentAsMarkdown(entry_id) match {
			case Some(content) => Ok(content)
			case None => NotFound
		}
	}

	def updateEntry(entry_id: Long) = IsEditor { user => implicit request =>
		newEntryForm.bindFromRequest.fold(
			formWithErrors => BadRequest(""),
			{ case (content) => {
				Entry.load(entry_id) match {
					case Some(entry) => {						
						Logger.debug("Update entry with text '"+content+"'")
						Entry.edit(entry.id, content)
						Cache.remove("view."+entry.stmt_id)
						Ok("")
					}
					case None => 
						NotFound
				}
			}}
		)
	}	

	def deleteEntry(entry_id: Long) = IsEditor { user => implicit request =>
		Entry.load(entry_id) match {
			case Some(entry) => {
				Entry.delete(entry_id)
				Cache.remove("view."+entry.stmt_id)
				Ok("")
			}
			case None => 
				NotFound
		}
	}	

	val newTagForm = Form("name" -> nonEmptyText)
	def addTag(id: Long) = IsEditor { user => implicit request => 
		newTagForm.bindFromRequest.fold(
			formWithErrors => BadRequest(""),
			{
				case (name) => {
					val tag = (Tag.load(name) match {
						case Some(tag) => tag
						case None => Tag.create(name)
					})
					Tag.add(id, tag)
					Ok("")
				}
			}
		)
	}

	def deleteTag(stmt_id: Long, tagid: Long) = IsEditor { user => implicit request => 
		Tag.delete(stmt_id, tagid)
		Ok("")
	}
}