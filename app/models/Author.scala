package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

/**
  *	Describes an author of a [[Statement]], i.e., who made this statement.
  * @param name the author's name
  * @param order lists of authors will be ordered by this value in ascending order
  * @param top_level when true, this author is a top-level author. Authors form a two-level hierarchy. 
  *			[[Statement]]s by subordinate authors can link to a [[Statement]] from a top-level author. 
  *			A top-level author may be e.g. the ruling government or its coalition treaty, 
  *			 and the subordinate authors may be the election programs of the governing parties. 
  *			 You can link the campaign statements of the governing parties to the final coalition treaty of 
  *			 the government. By default, the subordinate statements will display the rating of the 
  *			 linked top-level statement. This can be overridden for each statement.
  * @param color text color in labels as a hex string, e.g. "#ffffff"
  * @param background background color for labels as a hex string, e.g., "#000000"
*/
case class Author(id: Long, name: String, order: Long, top_level: Boolean, color: String, background: String) {
	// TODO: Add shortname to database
	def shortname : String = name.takeWhile(_.isLetter)
}

object Author {
	val author = {
		get[Long]("id") ~
		get[String]("name") ~
		get[Long]("ordering") ~
		get[Boolean]("top_level") ~
		get[String]("color") ~
		get[String]("background") map {
			case id ~ name ~ ordering ~ top_level ~ color ~ background => Author(id, name, ordering, top_level, color, background)
		}
	}

	def create(name: String, order: Long, top_level: Boolean, color: String, background: String): Author = {
		DB.withConnection { implicit c => create(c, name, order, top_level, color, background) }
	}

	def create(implicit connection: java.sql.Connection, name: String, order: Long, top_level: Boolean, color: String, background: String): Author = {
		val id = SQL("INSERT INTO author VALUES (DEFAULT, {name}, {order}, {top_level}, {color}, {background}) RETURNING id").on(
			'name -> name, 'order -> order, 'top_level -> top_level, 'color -> color, 'background -> background
			).as(scalar[Long].single)
		Author(id, name, order, top_level, color, background)
	}

	def edit(id: Long, name: String, order: Long, top_level: Boolean, color: String, background: String) : Boolean = {
		DB.withConnection { implicit c =>
			0 < SQL("""UPDATE author SET name = {name}, ordering = {ordering}, 
				top_level = {top_level}, color = {color}, background = {background} WHERE id = {id}""").on(
				'id -> id, 'name -> name, 'ordering -> order, 'top_level -> top_level, 'color -> color, 'background -> background
				).executeUpdate()
		}
	}

	def load(name : String): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM author WHERE name = {name}").on('name -> name).as(author.singleOpt)
		}
	}

	def load(id : Long): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM author WHERE id = {id}").on('id -> id).as(author.singleOpt)
		}
	}

	def loadTopLevel(): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM author WHERE top_level = TRUE ORDER BY ordering ASC LIMIT 1").as(author.singleOpt)
		}
	}

	def loadAll(): List[Author] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM author ORDER BY ordering ASC").as(author*)
		}
	}

	import play.api.libs.json._
	implicit val AuthorToJson = new Writes[Author] {
	  def writes(a: Author): JsValue = {
	    Json.obj(
	    	"id" -> a.id,
	    	"name" -> a.name,
	    	"ordering" -> a.order,
	    	"top_level" -> a.top_level,
	    	"color" -> a.color,
	    	"background" -> a.background
	    )
	  }
	}
}