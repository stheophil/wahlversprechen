package controllers

import models._
import models.Rating._

import play.api.cache._
import play.api.data._
import play.api.data.Forms._
import play.api.db._
import play.api.mvc._
import play.api.Logger
import play.api.Play.current

import views._

class ValidationException extends RuntimeException

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
	
	import play.api.libs.json._
	def viewAsJSON(id: Long) = CachedAction("view.json."+id, 60 * 60) { implicit request =>	
		Statement.load(id) match {
			case None => NotFound
			case Some(stmt) => Ok(Json.toJson(Statement.withEntries(stmt)))
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
			"title" -> optional(text),
			"rating" -> optional(number(min=0, max=Rating.maxId-1)),
			"quote" -> optional(text),
			"quote_src" -> optional(text),
			"merged_id" -> optional(number)
		) 
	)

	def update(stmt_id: Long) = IsEditor { user => implicit request =>
		try {
			updateItemForm.bindFromRequest.fold(
				formWithErrors => throw new ValidationException(),
				{ case (title, rating, quote, quote_src, merged_id) => {
					Logger.debug("Update item " + stmt_id + " (" + title + ", " + rating + ", " + quote + ", " + quote_src + ", " + merged_id + ")" )

					DB.withTransaction{ implicit c => 
						if(!title.map( Statement.setTitle(c, stmt_id, _) ).getOrElse(true) ||
						!rating.map( r => Statement.setRating(c, stmt_id, Rating(r), new java.util.Date()) ).getOrElse(true) ||
						!quote.map( Statement.setQuote(c, stmt_id, _) ).getOrElse(true) ||
						!quote_src.map( Statement.setQuoteSrc(c, stmt_id, _) ).getOrElse(true) ||
						!merged_id.map( Statement.setMergedID(c, stmt_id, _) ).getOrElse(true))
						{
							throw new ValidationException()
						}
						Cache.remove("view."+stmt_id)
						Ok("")						
					}
				}}
			)
		} catch {
			case e: ValidationException => BadRequest("")
		} 
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