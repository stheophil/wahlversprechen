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
import CacheFormat._

class ValidationException extends RuntimeException

object DetailViewController extends Controller with Secured with Cached {
	override val cachePrefix = "view"

	def view(id: Long) = CachedEternal(id, CacheFormat.HTML) { implicit request =>		
		Statement.load(id) match {
			case Some(stmt) => 
				Ok(views.html.detail(
					Statement.withEntriesAndRatings(stmt), 
					if(stmt.author.top_level) {
						Statement.loadAll(id)
					} else if(stmt.linked_id.isDefined) {
						Statement.loadAll(stmt.linked_id.get)
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
	def viewAsJSON(id: Long) = CachedEternal(id, CacheFormat.JSON) { implicit request =>	
		Statement.load(id) match {
			case None => NotFound
			case Some(stmt) => Ok(Json.toJson(Statement.withEntriesAndRatings(stmt)))
		}
	}

	def viewAsFeed(id: Long) = CachedEternal(id, CacheFormat.RSS) { implicit request =>
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

	def relatedUrlsAsJSON(id: Long, limit: Option[Int], offset: Option[Int] ) = CachedAction("relatedurl."+id+"."+limit+"."+offset) { implicit request => 
		Ok(Json.toJson( RelatedUrl.load( id, limit, offset)))
	}

	val newEntryForm = Form("content" -> nonEmptyText)

	def addEntry(stmt_id: Long) = IsEditor { user => implicit request =>
		newEntryForm.bindFromRequest.fold(
			formWithErrors => BadRequest(""),
			{ case (content) => {
				Entry.create(stmt_id, content, new java.util.Date(), user.id)
				invalidateCaches(stmt_id)
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
			"linked_id" -> optional(number)
		) 
	)

	def update(stmt_id: Long) = IsEditor { user => implicit request =>
		try {
			updateItemForm.bindFromRequest.fold(
				formWithErrors => throw new ValidationException(),
				{ case (title, rating, quote, quote_src, linked_id) => {
					Logger.debug("Update item " + stmt_id + " (" + title + ", " + rating + ", " + quote + ", " + quote_src + ", " + linked_id + ")" )

					if(user.role==Role.Editor 
					&& (title.isDefined || quote.isDefined || quote_src.isDefined || linked_id.isDefined)) {
						Forbidden
					} else {						
						DB.withTransaction{ implicit c => 
							if(!title.map( Statement.setTitle(c, stmt_id, _) ).getOrElse(true) ||
							!rating.map( r => Statement.setRating(c, stmt_id, Rating(r)) ).getOrElse(true) ||
							!quote.map( Statement.setQuote(c, stmt_id, _) ).getOrElse(true) ||
							!quote_src.map( Statement.setQuoteSrc(c, stmt_id, _) ).getOrElse(true) ||
							!linked_id.map( Statement.setLinkedID(c, stmt_id, _) ).getOrElse(true))
							{
								throw new ValidationException()
							}
							invalidateCaches(stmt_id)
							Ok("")						
						}	
					}
				}}
			)
		} catch {
			case e: ValidationException => BadRequest("")
		} 
	}

	def delete(stmt_id: Long) = IsAdmin { user => implicit request =>
		Statement.delete(stmt_id)
		invalidateCaches(stmt_id)
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
						if(user.role==Role.Admin || entry.user==user) {
							Entry.edit(entry.id, content)
							invalidateCaches(entry.stmt_id)
							Ok("")
						} else {
							Forbidden
						}
					}
					case None => 
						NotFound
				}
			}}
		)
	}	

	def deleteEntry(entry_id: Long) = IsAdmin { user => implicit request =>
		Entry.load(entry_id) match {
			case Some(entry) => {
				Entry.delete(entry_id)
				invalidateCaches(entry.stmt_id)
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
					invalidateCaches(id)
					Ok("")
				}
			}
		)
	}

	def deleteTag(stmt_id: Long, tagid: Long) = IsEditor { user => implicit request => 
		Tag.delete(stmt_id, tagid)
		invalidateCaches(stmt_id)
		Ok("")
	}
}