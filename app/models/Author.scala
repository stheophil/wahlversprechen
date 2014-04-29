package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

/**
  *	Describes an author of a [[Statement]], i.e., who made this statement.
  * @param name the author's name
  * @param order lists of authors will be ordered by this value in ascending order
  * @param rated when true, this author is the root author. [[Statement]]s by non-rated authors
  *				 can reference a [[Statement]] from the single rated author, thus forming a two-level
  *				 tree. The root author may be e.g. the ruling government or its coalition treaty, 
  *				 and the subordinate non-rated authors may be the election programs of the governing parties. 
  *				 You can link the campaign statements of the governing parties to the final coalition treaty of 
  *				 the government. The coalition treaty is the program the government acts upon and therefore this
  *				 is the main author to be "rated".
  * @param color text color in labels as a hex string, e.g. "#ffffff"
  * @param background background color for labels as a hex string, e.g., "#000000"
*/
case class Author(id: Long, name: String, order: Long, rated: Boolean, color: String, background: String)

object Author {
	val author = {
		get[Long]("id") ~
		get[String]("name") ~
		get[Long]("ordering") ~
		get[Boolean]("rated") ~
		get[String]("color") ~
		get[String]("background") map {
			case id ~ name ~ ordering ~ rated ~ color ~ background => Author(id, name, ordering, rated, color, background)
		}
	}

	def create(name: String, order: Long, rated: Boolean, color: String, background: String): Author = {
		DB.withConnection { implicit c => create(c, name, order, rated, color, background) }
	}

	def create(implicit connection: java.sql.Connection, name: String, order: Long, rated: Boolean, color: String, background: String): Author = {
		val id = SQL("INSERT INTO author VALUES (DEFAULT, {name}, {order}, {rated}, {color}, {background}) RETURNING id").on(
			'name -> name, 'order -> order, 'rated -> rated, 'color -> color, 'background -> background
			).as(scalar[Long].single)
		Author(id, name, order, rated, color, background)
	}

	def edit(id: Long, name: String, order: Long, rated: Boolean, color: String, background: String) : Boolean = {
		DB.withConnection { implicit c =>
			0 < SQL("""UPDATE author SET name = {name}, ordering = {ordering}, 
				rated = {rated}, color = {color}, background = {background} WHERE id = {id}""").on(
				'id -> id, 'name -> name, 'ordering -> order, 'rated -> rated, 'color -> color, 'background -> background
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

	def loadRated(): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM author WHERE rated = TRUE ORDER BY ordering ASC LIMIT 1").as(author.singleOpt)
		}
	}

	def loadAll(): List[Author] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM author ORDER BY ordering ASC").as(author*)
		}
	}
}