package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import java.util.Date

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

	def loadByStatement(stmt_id: Long): List[Entry] = {
		DB.withConnection { implicit c =>
			SQL("select * from entry where stmt_id = {stmt_id} ORDER by date DESC").on('stmt_id -> stmt_id).as(entry*)
		}
	}

	def loadRecent(limit: Long): List[Entry] = {
		DB.withConnection { implicit c =>
			SQL("select * from entry ORDER by date DESC limit {limit}").on('limit -> limit).as(entry*)
		}
	}

	def contentAsMarkdown(id: Long) : Option[String] = {
		DB.withConnection { implicit c => 
			SQL("select content from entry where id = {id}").on('id -> id).as(scalar[String].singleOpt)
		}
	}

	def edit(stmt_id: Long, id: Long, content: String, date: Date, user_id: Long) {
		DB.withTransaction { implicit c =>
			SQL("update entry set content = {content}, date = {date}, user_id = {user_id} where id = {id}").on(
				'content -> content,
				'date -> date,
				'user_id -> user_id,
				'id -> id).executeUpdate()

         	SQL("update statement set latestEntry = {date} where id = {stmt_id}").on(					
					'date -> date,
					'stmt_id -> stmt_id).executeUpdate()
		}
	}

	def delete(stmt_id: Long, id: Long) {
		DB.withTransaction { implicit c =>
			SQL("delete entry where id = {id} and stmt_id = {stmt_id}").on('id -> id, 'stmt_id -> stmt_id).executeUpdate()

			val latest = SQL("select MAX(date) from entry where stmt_id = {stmt_id}").on('stmt_id -> stmt_id).as(scalar[Date].singleOpt)
         	SQL("update statement set latestEntry = {date} where id = {stmt_id}").on(					
					'date -> latest,
					'stmt_id -> stmt_id).executeUpdate()
		}	
	}

	def create(stmt_id: Long, content: String, date: Date, user_id: Long) {
		DB.withTransaction { implicit c =>
			val id = SQL("select nextval('entry_id_seq')").as(scalar[Long].single)

			SQL("insert into entry values ({id}, {stmt_id}, {content}, {date}, {user_id})").on(
					'id -> id,
					'stmt_id -> stmt_id,
					'content -> content,
					'date -> date,
					'user_id -> user_id).executeUpdate()

         	SQL("update statement set latestEntry = {date} where id = {stmt_id}").on(					
					'date -> date,
					'stmt_id -> stmt_id).executeUpdate()
		}
	}
}