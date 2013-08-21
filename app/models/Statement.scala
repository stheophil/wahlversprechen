package models

object Rating extends Enumeration {
  type Rating = Value
  val PromiseKept, Compromise, PromiseBroken, Stalled, InTheWorks, Unrated = Value
};

object Role extends Enumeration {
  type Role = Value
  val Admin, Editor, Unprivileged = Value
};

import Role._
import Rating._

import java.util.Date

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current
import play.api.templates._

case class Category(id: Long, name: String, order: Long)
case class User(id: Long, email: String, name: String, password: String, salt: String, role: Role)
case class Entry(id: Long, stmt_id: Long, content: String, date: Date, user: User)
case class Statement(id: Long, title: String, category: Category, entries: List[Entry], rating: Rating)


object Category {  
	def create(name: String, order: Long) : Category = {
		DB.withConnection { implicit c =>
      val id: Long = SQL("select next value for cat_id_seq").as(scalar[Long].single)
	  
        SQL("insert into category values ({id}, {name}, {order})").on(
          'id -> id,
	        'name -> name,
	        'order -> order
	      ).executeUpdate()

        Category(id, name, order)
	    }
	} 
}

object User {  
  val user = {
    get[Long]("id") ~
	  get[String]("email") ~ 
	  get[String]("name") ~
	  get[String]("password") ~
    get[String]("salt") ~
	  get[Int]("role") map {
	    case id~email~name~password~salt~role => User(id, email, name, password, salt, if(0<=role && role<Role.maxId) Role(role) else Unprivileged)
	  }
  }
  
  def load(id: Long) : Option[User] = {
    DB.withConnection { implicit c =>
      SQL("select * from users where id = {id}").on('id -> id).as(user.singleOpt)
    }
  }

  def load(email: String) : Option[User] = {
    DB.withConnection { implicit c =>
      SQL("select * from users where email = {email}").on('email -> email).as(user.singleOpt)
    }
  }
  
  def findAll() : List[User] = {
    DB.withConnection { implicit c =>
      SQL("select * from users").as(user*)
    }
  }

  private def passwordhash(salt: String, password: String) : String = {
     val md = java.security.MessageDigest.getInstance("SHA-1")
     new sun.misc.BASE64Encoder().encode( md.digest( (salt + password).getBytes) )
  }
  
  def create(email: String, name: String, password: String, role: Role) : User = {
    DB.withConnection { implicit c =>
      val id: Long = SQL("select next value for user_id_seq").as(scalar[Long].single)
      val salt = (for(i <- 1 to 20) yield util.Random.nextPrintableChar).mkString
      val hash = passwordhash(salt, password)
      SQL("insert into users values ({id}, {email}, {name}, {password}, {salt}, {role})").on(
        'id -> id,
        'email -> email,
        'name -> name,
        'password -> hash,
        'salt -> salt,
        'role -> role.id
      ).executeUpdate()

      User(id, email, name, hash, salt, role)
    }
  }
  
  def authenticate(email: String, password: String): Option[User] = {
    DB.withConnection { implicit connection =>
      User.load(email) filter (u => u.password == passwordhash(u.salt, password))
    }
  }
}  

object Entry {
  val entry = {
    get[Long]("id") ~
    get[Long]("stmt_id") ~ 
    get[String]("content") ~
    get[Date]("date") ~
    get[Long]("user_id") map {
      case id~stmt_id~content~date~user_id => Entry(id, stmt_id, content, date, User.load(user_id).get)
    }
  }
  
  def loadByStatement(stmt_id: Long) : List[Entry] = {
    DB.withConnection { implicit c =>
      SQL("select * from entry where stmt_id = {stmt_id} ORDER by date DESC").on('stmt_id -> stmt_id).as(entry*)
    }
  }
  
  def create(stmt_id: Long, content: String, date: Date, user_id: Long) {
    DB.withTransaction { implicit c =>
       val id = SQL("select next value for entry_id_seq").as(scalar[Long].single)
       
       SQL(
         """
           insert into entry values (
             {id}, {stmt_id}, {content}, {date}, {user_id}
           )
         """
       ).on(
         'id -> id,
         'stmt_id -> stmt_id,
         'content -> content,
         'date -> date,
         'user_id -> user_id
       ).executeUpdate()
     } 
  }
}

object Statement {  
  val stmt = {
	  get[Long]("id") ~ 
	  get[String]("title")  ~
    get[Long]("category.id") ~
	  get[String]("category.name") ~
	  get[Long]("category.ordering") ~
	  get[Int]("rating") map {
	    case id~title~category_id~category_name~category_order~rating => Statement(id, title, Category(category_id, category_name, category_order), List[Entry](), if(0<=rating && rating<Rating.maxId) Rating(rating) else Unrated)
	  } // Rating(rating) would throw java.util.NoSuchElementException
  }
  
  var query = """select statement.id, title, rating, category.id, category.name, category.ordering from statement join category on category.id=cat_id"""

  def allWithoutEntries(): List[Statement] = {
    DB.withConnection( { implicit c =>
      SQL(query + " order by category.ordering").as(stmt *)
     } )
  }
  
  def load(id: Long) : Option[Statement] = {
    // TODO: Create stmt with entries in the first place
    val s = DB.withConnection( { implicit c => 
      SQL(query + " where statement.id = {id}").on(
          'id -> id
      ).as(stmt.singleOpt)
    } )
    s map { stmt => Statement(stmt.id, stmt.title, stmt.category, Entry.loadByStatement(id), stmt.rating) }    
  } 
  
  def create(title: String, cat: Category, rating: Rating) : Statement = {
    DB.withTransaction { implicit c =>
       // Get the project id
       val id: Long = SQL("select next value for stmt_id_seq").as(scalar[Long].single)       
       // Insert the project
       SQL(
         """
           insert into statement values (
             {id}, {title}, {cat_id}, {rating}
           )
         """
       ).on(
         'id -> id,
         'title -> title,
         'cat_id -> cat.id,
         'rating -> rating.id
       ).executeUpdate()
       
       Statement(id, title, cat, List[Entry](), rating)
     }
  }
}
