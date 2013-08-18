package models

object Rating extends Enumeration {
  type Rating = Value
  val PromiseKept, Compromise, PromiseBroken, Stalled, InTheWorks, Unrated = Value
};
import Rating._

object Role extends Enumeration {
  type Role = Value
  val Admin, Editor, Unprivileged = Value
};
import Role._

import java.util.Date

import anorm._
import play.api.templates._

case class Category(name: String, order: Long)
case class User(email: String, name: String, password: String, role: Role)
case class Entry(id: Pk[Long], stmt_id: Pk[Long], content: Html, date: Date, user: User)
case class Statement(id: Pk[Long], title: String, category: Category, entries: List[Entry], rating: Rating)

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

object Category {  
	def create(cat: Category) : Category = {
		DB.withConnection { implicit c =>
	      SQL("insert into category values ({name}, {order})").on(
	        'name -> cat.name,
	        'order -> cat.order
	      ).executeUpdate()
	    }
		cat
	} 
}

object User {  
  val user = {
	  get[String]("email") ~ 
	  get[String]("name") ~
	  get[String]("password") ~
	  get[Int]("role") map {
	    case email~name~password~role => User(email, name, password, if(0<=role && role<Role.maxId) Role(role) else Unprivileged)
	  }
  }
  
  def load(email: String) : Option[User] = {
    DB.withConnection { implicit c =>
      SQL("select * from user where email = {email}").on('email -> email).as(user.singleOpt)
    }
  }
  
  def findAll() : List[User] = {
    DB.withConnection { implicit c =>
      SQL("select * from user").as(user*)
    }
  }
  
  def create(user: User) : User = {
    DB.withConnection { implicit c =>
      SQL("insert into user values ({email}, {name}, {password}, {role})").on(
        'email -> user.email,
        'name -> user.name,
        'password -> user.password,
        'role -> user.role.id
      ).executeUpdate()

      user      
    }
  }
  
  def authenticate(email: String, password: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
         select * from user where 
         email = {email} and password = {password}
        """
      ).on(
        'email -> email,
        'password -> password
      ).as(user.singleOpt)
    }
  }
}  

object Entry {
  val entry = {
    get[Pk[Long]]("id") ~
    get[Pk[Long]]("stmt_id") ~ 
    get[String]("content") ~
    get[Date]("date") ~
    get[String]("email") map {
      case id~stmt_id~content~date~email => Entry(id, stmt_id, Html(content), date, User.load(email).get)
    }
  }
  
  def loadByStatement(stmt_id: Long) : List[Entry] = {
    DB.withConnection { implicit c =>
      SQL("select * from entry where stmt_id = {stmt_id} ORDER by date DESC").on('stmt_id -> stmt_id).as(entry*)
    }
  }
  
  def create(stmt_id: Long, e: Entry) : Pk[Long] = {
    DB.withTransaction { implicit c =>
       val id: Long = e.id.getOrElse {
         SQL("select next value for entry_id_seq").as(scalar[Long].single)
       }
       
       SQL(
         """
           insert into entry values (
             {id}, {stmt_id}, {content}, {date}, {user}
           )
         """
       ).on(
         'id -> id,
         'stmt_id -> stmt_id,
         'content -> e.content.toString,
         'date -> e.date,
         'user -> e.user.email
       ).executeUpdate()
              
       Id(id)
     } 
  }
}

object Statement {  
  val stmt = {
	  get[Pk[Long]]("id") ~ 
	  get[String]("title")  ~
	  get[String]("category.name") ~
	  get[Long]("category.ordering") ~
	  get[Int]("rating") map {
	    case id~title~category_name~category_order~rating => Statement(id, title, Category(category_name, category_order), List[Entry](), if(0<=rating && rating<Rating.maxId) Rating(rating) else Unrated)
	  } // Rating(rating) would throw java.util.NoSuchElementException
  }
  
  var query = """select id, title, rating, name, ordering from statement join category on category.name=category"""

  def allWithoutEntries(): List[Statement] = {
    DB.withConnection( { implicit c =>
      SQL(query + " order by category.ordering").as(stmt *)
     } )
  }
  
  def load(id: Long) : Option[Statement] = {
    // TODO: Create stmt with entries in the first place
    val s = DB.withConnection( { implicit c => 
      SQL(query + " where id = {id}").on(
          'id -> id
      ).as(stmt.singleOpt)
    } )
    s map { stmt => Statement(stmt.id, stmt.title, stmt.category, Entry.loadByStatement(id), stmt.rating) }    
  } 
  
  def create(stmt: Statement) : Pk[Long] = {
    val id = DB.withTransaction { implicit c =>
       // Get the project id
       val id: Long = stmt.id.getOrElse {
         SQL("select next value for stmt_id_seq").as(scalar[Long].single)
       }
       
       // Insert the project
       SQL(
         """
           insert into statement values (
             {id}, {title}, {category}, {rating}
           )
         """
       ).on(
         'id -> id,
         'title -> stmt.title,
         'category -> stmt.category.name,
         'rating -> stmt.rating.id
       ).executeUpdate()
       Id(id)
     }
    if(id.isDefined) stmt.entries.foreach{ entry => Entry.create(id.get, entry) }
    id
  }
}
