package controllers

import com.google.gdata.client.spreadsheet._
import com.google.gdata.data.spreadsheet._
import com.google.gdata.util._

import models._
import models.Rating._

import play.api.Logger
import play.api.mvc._

import scala.collection.JavaConversions._

object Import extends Controller {
	def loadSpreadSheet(author_name: String, spreadsheet: String) : SimpleResult = {		
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
				val strLinks = if (author.top_level && custom.getValue("links") != null) {
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
								Statement.edit(c, id, importrow.title, category, importrow.quote, importrow.quote_source)
								Tag.eraseAll(c, id)
								id
							}
							case None => {
								cInserted += 1
								Logger.info("Create statement " + importrow.title + " with category " + importrow.category)					
								Statement.create(c, importrow.title, author, category, importrow.quote, importrow.quote_source).id
							}
						}
					}

					importrow.links.foreach(
							_.split(',').map(_.trim).distinct.foreach( link => {
								try {
									Statement.setLinkedID(c, java.lang.Integer.parseInt( link ), stmt_id)
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