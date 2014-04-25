package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

/**
  * Category of a [[Statement]], e.g., 'Foreign Affairs', 'Department of Labor' etc
  * @param name its name
  * @param order lists of categories are sorted by 'order' in ascending order
  */
case class Category(id: Long, name: String, order: Long)

object Category {
	val category = {
		get[Long]("id") ~
		get[String]("name") ~
		get[Long]("ordering") map {
			case id ~ name ~ ordering => Category(id, name, ordering)
		}
	}

	def create(name: String, order: Long): Category = {
		DB.withConnection { implicit c => create(c, name, order) }
	}

	def create(implicit connection: java.sql.Connection, name: String, order: Long): Category = {
		val id = SQL("INSERT INTO category VALUES (DEFAULT, {name}, {order}) RETURNING id").on(
			'name -> name, 'order -> order).as(scalar[Long].single)
		Category(id, name, order)
	}

	def loadAll(): List[Category] = {
		DB.withConnection { implicit c => loadAll(c) }
	}

	def loadAll(implicit connection: java.sql.Connection): List[Category] = {
		SQL("SELECT * FROM category ORDER BY ordering").as(category*)
	}

	import play.api.libs.json._
	implicit val CategoryToJson = new Writes[Category] {
	  def writes(c: Category): JsValue = {
	    Json.obj(
	    	"id" -> c.id,
	    	"name" -> c.name,
	    	"ordering" -> c.order
	    )
	  }
	}
}
