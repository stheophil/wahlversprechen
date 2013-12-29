package models

object Role extends Enumeration {
	type Role = Value
	val Admin, Editor, Unprivileged = Value
};

import Role._

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class User(id: Long, email: String, name: String, password: String, salt: String, role: Role)

object User {
	val user = {
		get[Long]("id") ~
			get[String]("email") ~
			get[String]("name") ~
			get[String]("password") ~
			get[String]("salt") ~
			get[Int]("role") map {
				case id ~ email ~ name ~ password ~ salt ~ role => User(id, email, name, password, salt, if (0 <= role && role < Role.maxId) Role(role) else Unprivileged)
			}
	}

	def load(id: Long): Option[User] = {
		DB.withConnection { implicit c =>
			SQL("select * from users where id = {id}").on('id -> id).as(user.singleOpt)
		}
	}

	def load(email: String): Option[User] = {
		DB.withConnection { implicit c =>
			SQL("select * from users where email = {email}").on('email -> email).as(user.singleOpt)
		}
	}

	def findAll(): List[User] = {
		DB.withConnection { implicit c =>
			SQL("select * from users order by id DESC").as(user*)
		}
	}

	private def passwordhash(salt: String, password: String): String = {
		val md = java.security.MessageDigest.getInstance("SHA-1")
		new sun.misc.BASE64Encoder().encode(md.digest((salt + password).getBytes))
	}

	def create(email: String, name: String, password: String, role: Role): User = {
		DB.withConnection { implicit c =>
			val id: Long = SQL("select nextval('user_id_seq')").as(scalar[Long].single)
			val salt = (for (i <- 1 to 20) yield util.Random.nextPrintableChar).mkString
			val hash = passwordhash(salt, password)
			SQL("insert into users values ({id}, {email}, {name}, {password}, {salt}, {role})").on(
				'id -> id,
				'email -> email,
				'name -> name,
				'password -> hash,
				'salt -> salt,
				'role -> role.id).executeUpdate()

			User(id, email, name, hash, salt, role)
		}
	}

	def edit(id: Long, email: String, name: String, password: Option[String], role: Option[Role]) {
		DB.withConnection { implicit c =>
			var query = "update users set email = {email}, name = {name}"
			var params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
			params += ('email -> email)
			params += ('name -> name)

			if(password.isDefined) {
				query += ", salt = {salt}, password = {password}"
				val salt = (for (i <- 1 to 20) yield util.Random.nextPrintableChar).mkString
				val hash = passwordhash(salt, password.get)

				params += ('salt -> salt)
				params += ('password -> hash)
			}

			if(role.isDefined) {
				query += ", role = {role}"
				params += ('role -> role.get)
			}
			query += " where id = {id}"
			params += ('id -> id)

			SQL(query).on(params:_*).executeUpdate
		}
	}

	def authenticate(email: String, password: String): Option[User] = {
		DB.withConnection { implicit connection =>
			User.load(email) filter (u => u.password == passwordhash(u.salt, password))
		}
	}
}