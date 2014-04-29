package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import java.util.Date

/**
  * A blog-like entry belonging to a [[Statement]], appears on the detail page for a statement. 
  * @param stmt_id the id of the statement this text belongs to 
  * @param content the text in Markdown syntax
  * @param date the date when this entry was written
  * @param the user who wrote this entry
  */
case class Entry(id: Long, stmt_id: Long, content: String, date: Date, user: User)

object Entry {
	val entry = {
		get[Long]("id") ~
			get[Long]("stmt_id") ~
			get[String]("content") ~
			get[Date]("date") ~
			get[Long]("user_id") map {
				case id ~ stmt_id ~ content ~ date ~ user_id => Entry(id, stmt_id, content, date, User.load(user_id).get)
			}
	}

	def load(id: Long): Option[Entry] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM entry WHERE id = {id} ORDER BY date DESC").on('id -> id).as(entry.singleOpt)
		}
	}

	def loadByStatement(stmt_id: Long): List[Entry] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM entry WHERE stmt_id = {stmt_id} ORDER BY date DESC").on('stmt_id -> stmt_id).as(entry*)
		}
	}

	def loadRecent(limit: Long): List[Entry] = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM entry ORDER BY date DESC LIMIT {limit}").on('limit -> limit).as(entry*)
		}
	}

	def contentAsMarkdown(id: Long) : Option[String] = {
		DB.withConnection { implicit c => 
			SQL("SELECT content FROM entry WHERE id = {id}").on('id -> id).as(scalar[String].singleOpt)
		}
	}

	def edit(id: Long, content: String) : Boolean = {
		DB.withConnection { implicit c =>
			0 < SQL("UPDATE entry SET content = {content} WHERE id = {id}").on(
				'content -> content, 'id -> id).executeUpdate()
		}
	}

	def delete(id: Long) : Boolean = {
		DB.withConnection { implicit c =>
			0 < SQL("DELETE FROM entry WHERE id = {id}").on('id -> id).executeUpdate()
		}	
	}

	def create(stmt_id: Long, content: String, date: Date, user_id: Long) : Long = {
		DB.withConnection { implicit c =>
			SQL("INSERT INTO entry VALUES (DEFAULT, {stmt_id}, {content}, {date}, {user_id}) RETURNING id").on(
				'stmt_id -> stmt_id, 'content -> content, 'date -> date, 'user_id -> user_id
			).as(scalar[Long].single)
		}
	}
}