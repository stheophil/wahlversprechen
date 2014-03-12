package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

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
		val id: Long = SQL("select nextval('cat_id_seq')").as(scalar[Long].single)

		SQL("insert into category values ({id}, {name}, {order})").on(
			'id -> id,
			'name -> name,
			'order -> order).executeUpdate()

		Category(id, name, order)
	}

	def loadAll(): List[Category] = {
		DB.withConnection { implicit c => loadAll(c) }
	}

	def loadAll(implicit connection: java.sql.Connection): List[Category] = {
		SQL("select * from category order by ordering").as(category*)
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
