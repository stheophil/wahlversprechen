package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Tag(id: Long, name: String, important: Boolean)

object Tag {
	val tag = {
		get[Long]("id") ~
		get[String]("name") ~
		get[Boolean]("important") map {
			case id ~ name ~ important => Tag(id, name, important)
		}
	}

	def create(name: String): Tag = {
		DB.withConnection { implicit c => create(name) }
	}

	def create(implicit connection: java.sql.Connection, name: String): Tag = {
		val id: Long = SQL("select nextval('tag_id_seq')").as(scalar[Long].single)

		SQL("insert into tag values ({id}, {name})").on('id -> id, 'name -> name).executeUpdate()
		Tag(id, name, false)
	}

	def loadByStatement(stmt_id: Long): List[Tag] = {
		DB.withConnection { implicit c =>
			SQL("""select id, name, important from tag 
			join statement_tags on statement_tags.tag_id=id 
			where statement_tags.stmt_id = {stmt_id} 
			order by tag.name""").on('stmt_id -> stmt_id).as(tag*)
		}
	}

	def load(id: Long): Option[Tag] = {
		DB.withConnection { implicit c =>
			SQL("select id, name, important from tag where tag.id={id}").on('id -> id).as(tag.singleOpt)
		}
	}

	def load(implicit connection: java.sql.Connection, name: String): Option[Tag] = {
		SQL("select id, name, important from tag where tag.name={name}").on('name -> name).as(tag.singleOpt)
	}

	def load(name: String): Option[Tag] = {
		DB.withConnection { implicit c => load(c, name)	}
	}

	def loadAll(): List[Tag] = {
		DB.withConnection { implicit c => loadAll(c) }
	}

	def loadAll(implicit connection: java.sql.Connection): List[Tag] = {
			SQL("select id, name, important from tag order by name").as(tag*)
	}

	def add(implicit connection: java.sql.Connection, stmt_id: Long, tag: Tag) {
			SQL("insert into statement_tags values ({tag_id}, {stmt_id})").on(
				'tag_id -> tag.id,
				'stmt_id -> stmt_id
			).executeUpdate
	}

	def eraseAll(implicit connection: java.sql.Connection, stmt_id: Long) {
			SQL("delete from statement_tags where stmt_id = {stmt_id}").on(
				'stmt_id -> stmt_id
			).executeUpdate
	}

	def setImportant(tag_id: Long, important: Boolean) {
		DB.withConnection { implicit c =>
			SQL("update tag set important = {important} where id = {tag_id}").on(
				'important -> important,
				'tag_id -> tag_id
			).executeUpdate
		}	
	}

	def setName(tag_id: Long, name: String) {
		DB.withTransaction { implicit c =>
			val tag = load(c, name)
			if(tag.isDefined) { // name already exists -> merge tags
				if(tag.get.id!=tag_id) {
					SQL("update statement_tags set tag_id = {new_tag_id} where tag_id = {old_tag_id}").on(
						'old_tag_id -> tag_id,
						'new_tag_id -> tag.get.id
					).executeUpdate
					SQL("delete from tag where id = {tag_id}").on(
						'tag_id -> tag_id
					).executeUpdate				
				}
			} else {
				SQL("update tag set name = {name} where id = {tag_id}").on(
					'name -> name,
					'tag_id -> tag_id
				).executeUpdate
			}
		}	
	}

	def delete(tag_id: Long) {
		DB.withTransaction { implicit c =>
			SQL("delete from statement_tags where tag_id = {tag_id}").on(
				'tag_id -> tag_id
			).executeUpdate

			SQL("delete from tag where id = {tag_id}").on(
				'tag_id -> tag_id
			).executeUpdate
		}	
	}
}