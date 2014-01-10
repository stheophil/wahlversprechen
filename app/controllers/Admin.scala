package controllers

import com.google.gdata.client.spreadsheet._
import com.google.gdata.data.spreadsheet._
import com.google.gdata.util._

import models._
import models.Rating._

import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

import scala.collection.JavaConversions._

import views._

object Admin extends Controller with Secured {
	
	val editAuthorForm = Form(
		tuple(
			"id" -> number.verifying("Unbekannter Autor", author_id => Author.load(author_id).isDefined),
			"name" -> text, 
			"order" -> number,
			"rated" -> boolean, 
			"color" -> text,
			"background" -> text 
			)
		)

	def prefs = IsAdmin { user => implicit request => 
		Ok(html.adminPrefs( Author.loadAll(), editAuthorForm, User.findAll(), Tag.loadAll(), user ))
	}

	def userForm(edit: Boolean) = Form(
		tuple(
			"email" -> email,
			"name" -> nonEmptyText,
			"password" -> text.verifying(
				"Das Passwort muss mindestens 8 Stellen, eine Zahl und ein Sonderzeichen enthalten.", 
				password => edit && password.isEmpty 
				 		|| 8<=password.length && password.exists(_.isDigit) && password.exists(!_.isLetterOrDigit)
			),
			"role" -> number(min=0, max = Role.maxId),
			"admin_email" -> email,
			"admin_password" -> text
		) verifying ("Ungültige Administrator E-Mail oder falsches Passwort", result => result match {
			case (_, _, password, role, admin_email, admin_password) => {
				User.authenticate(admin_email, admin_password).map(_.role == Role.Admin).getOrElse(false)
			}
		}))

	def newUser = IsAdmin { user => implicit request => 
		userForm(/*edit*/ false).bindFromRequest.fold(
			formWithErrors => BadRequest(formWithErrors.errors.head.message),
			{ case (email, name, password, role, _, _) => {
				User.create(email, name, password, Role(role))
				Ok("")
			} }
		) 
	}

	def editUser(id: Long) = IsAdmin { user => implicit request => 
		userForm(/*edit*/ true).bindFromRequest.fold(
			formWithErrors => BadRequest(formWithErrors.errors.head.message),
			{ case (email, name, password, role, _, _) => {
				User.edit(id, email, name, Some(password), Some(Role(role))) 
				Ok("")
			} }
		) 
	}

	val verifyAdminForm = Form(
		tuple(
			"admin_email" -> email,
			"admin_password" -> text
		) verifying ("Ungültige Administrator E-Mail oder falsches Passwort", result => result match {
				case (admin_email, admin_password) => {
					User.authenticate(admin_email, admin_password).map(_.role == Role.Admin).getOrElse(false)
				}
		}))

	def deleteUser(id: Long) = IsAdmin { user => implicit request => 
		verifyAdminForm.bindFromRequest.fold(
			formWithErrors => BadRequest(formWithErrors.errors.head.message),
			{ case(_, _) => {
				User.delete(id)
				Ok("")
			}}
		) 
	}

	def editAuthor = IsAdmin { user => implicit request => 
		editAuthorForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.adminPrefs( Author.loadAll(), formWithErrors, User.findAll(), Tag.loadAll(), user)),
			{ case (id, name, order, rated, color, background) => {
				Author.edit(id, name, order, rated, color, background) 
				Redirect(routes.Admin.prefs.url + "#author").flashing("author_success" -> {"Änderungen an "+name+" erfolgreich gespeichert"})
			} }
		) // bindFromRequest
	}

	val updateTagForm = Form(
		tuple(
			"name" -> optional(text), 
			"important" -> optional(boolean)
		))

	def updateTag(id: Long) = IsAdmin { user => implicit request =>
		Logger.debug("updateTag: " + id)
		updateTagForm.bindFromRequest.fold(
			formWithErrors => InternalServerError(""),
			{
				case (name, important) => {
					if(important.isDefined) Tag.setImportant(id, important.get)
					if(name.isDefined) Tag.setName(id, name.get)
				}
			})
		Ok("")
	}

	def deleteTag(id: Long) = IsAdmin { user => implicit request =>
		Tag.delete(id)
		Ok("")
	}

	val importForm = Form(
		tuple(
			"author" -> text.verifying("Unbekannter Autor", author => Author.load(author).isDefined),
			"spreadsheet" -> nonEmptyText))

	def viewImportForm  = IsAdmin { user => implicit request =>
		Ok(html.importview(importForm, user))
	}

	def doImport = IsAdmin { user => implicit request =>
		importForm.bindFromRequest.fold(
			formWithErrors => BadRequest(""),
			{ case (author_name, spreadsheet) => loadSpreadSheet(author_name, spreadsheet) }
		) // bindFromRequest
	}
	
	def loadSpreadSheet(author_name: String, spreadsheet: String) : Result = {		
		class ImportException(message: String) extends java.lang.Exception(message)
		case class ImportRow(title: String, category: String, quote: Option[String], quote_source: Option[String], tags: Option[String], links: Option[String])
		
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

				val strCategory = if(custom.getValue("ressort")==null) None else Some(custom.getValue("ressort").trim)
				val strTitle = if(custom.getValue("titel")==null) None else Some(custom.getValue("titel").trim)
				
				val strQuote = if (custom.getValue("zitat") == null) None else Some(custom.getValue("zitat").trim)
				val strSource = if (custom.getValue("quelle") == null) None else Some(custom.getValue("quelle").trim)
				val strTags = if (custom.getValue("tags") == null) None else Some(custom.getValue("tags").trim)
				val strLinks = if (author.rated && custom.getValue("links") != null) {
						Some(custom.getValue("links")) }
					else {
						None
					}

				if(strCategory.isDefined || strTitle.isDefined || strQuote.isDefined ||
					strSource.isDefined || strTags.isDefined || strLinks.isDefined) 
				{
					
					if (!strTitle.isDefined) throw new ImportException("Fehlender Titel bei Wahlversprechen Nr. "+(cRows.length+1))
					if (!strCategory.isDefined) throw new ImportException("Fehlendes Ressort bei Wahlversprechen Nr. "+(cRows.length+1))

					Logger.info("Found statement " + strTitle)
					cRows += new ImportRow(strTitle.get, strCategory.get, strQuote, strSource, strTags, strLinks)
				} else {
					// skip completely empty rows
				}
			}

			Logger.info("Found " + cRows.length + " statements. Begin import.")

			import play.api.Play.current

			var cUpdated = 0
			var cInserted = 0
			play.api.db.DB.withTransaction { c =>

				var mapcategory = collection.mutable.Map.empty[String, Category]
				mapcategory ++= (for(c <- Category.loadAll(c)) yield (c.name, c))
				var nCategoryOrder = if(mapcategory.isEmpty) 0 else mapcategory.values.maxBy(_.order).order

				var maptag = collection.mutable.Map.empty[String, Tag]
				maptag ++= (for( t <- Tag.loadAll(c) ) yield (t.name, t))

				val mapstmtidByTitle = Statement.all().getOrElse( 
					author, 
					List.empty[Statement] 
				).map( stmt => stmt.title -> stmt.id ).toMap

				cRows.foreach(importrow => {

					val category = mapcategory.getOrElseUpdate(
						importrow.category,
						{
							nCategoryOrder = nCategoryOrder + 1
							Logger.info("Create category " + importrow.category + " with order " + nCategoryOrder)
							Category.create(c, importrow.category, nCategoryOrder)
						}
					)

					val stmt_id = { 
						mapstmtidByTitle.get(importrow.title) match {
							case Some(id) => {
								Logger.info("Update statement " + importrow.title + " with category " + importrow.category)
								cUpdated += 1
								Statement.edit(c, id, importrow.title, category, importrow.quote, importrow.quote_source, if(author.rated) Some(Rating.Unrated) else None, None)
								Tag.eraseAll(c, id)
								id
							}
							case None => {
								cInserted += 1
								Logger.info("Create statement " + importrow.title + " with category " + importrow.category)					
								Statement.create(c, importrow.title, author, category, importrow.quote, importrow.quote_source, if(author.rated) Some(Rating.Unrated) else None, None).id
							}
						}
					}

					importrow.links.foreach(
							_.split(',').map(_.trim).distinct.foreach( link => {
								try {
									Statement.setMergedID(c, java.lang.Integer.parseInt( link ), stmt_id)
								} catch {
									case e: NumberFormatException => 
										Logger.info("Illegal characters in linked statement id: " + link) 
								}
							})
					)

					importrow.tags.foreach( 
							_.split(',').map(_.trim).distinct.foreach( tagname => {
								val tag = maptag.getOrElseUpdate(tagname, { Tag.create(c, tagname) })
								Tag.add(c, stmt_id, tag)
							})
					)
				})
			}

			Ok(cInserted+" Wahlversprechen erfolgreich hinzugefügt, " + cUpdated + " Wahlversprechen erfolgreich aktualisiert.")
		} catch {
			case e: com.google.gdata.util.ServiceException => {
				BadRequest("Fehler beim Zugriff auf das Google Spreadsheet. Google meldet folgendes: " + e.getMessage())
			}
			case e: ImportException => {
				BadRequest("Die Daten im Google Spreadsheet sind fehlerhaft: " + e.getMessage())
			}
			case e: Exception => {
				Logger.error(e.toString)
				Logger.error(e.getStackTraceString)
				InternalServerError("")
			}
		}		
	}
}