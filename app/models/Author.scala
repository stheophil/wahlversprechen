package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

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
		val id: Long = SQL("select nextval('author_id_seq')").as(scalar[Long].single)

		SQL("insert into author values ({id}, {name}, {order}, {rated}, {color}, {background})").on(
			'id -> id,
			'name -> name,
			'order -> order,
			'rated -> rated,
			'color -> color,
			'background -> background).executeUpdate()

		Author(id, name, order, rated, color, background)
	}

	def edit(id: Long, name: String, order: Long, rated: Boolean, color: String, background: String) {
		DB.withConnection { implicit c =>

			SQL("update author set name = {name}, ordering = {ordering}, rated = {rated}, color = {color}, background = {background} where id = {id}").on(
				'id -> id,
				'name -> name,
				'ordering -> order,
				'rated -> rated,
				'color -> color,
				'background -> background).executeUpdate()
		}
	}

	def load(name : String): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("select * from author where name = {name}").on('name -> name).as(author.singleOpt)
		}
	}

	def load(id : Int): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("select * from author where id = {id}").on('id -> id).as(author.singleOpt)
		}
	}

	def loadRated(): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("select * from author where rated = TRUE order by ordering asc limit 1").as(author.singleOpt)
		}
	}

	def loadAll(): List[Author] = {
		DB.withConnection { implicit c =>
			SQL("select * from author order by ordering asc").as(author*)
		}
	}
}