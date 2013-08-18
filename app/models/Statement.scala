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

case class Category(id: Pk[Long], name: String, order: Long)
case class User(id: Pk[Long], email: String, name: String, password: String, role: Role)
case class Entry(id: Pk[Long], stmt_id: Pk[Long], content: String, date: Date, user: User)
case class Statement(id: Pk[Long], title: String, category: Category, entries: List[Entry], rating: Rating)


object Category {  
	def create(cat: Category) : Pk[Long] = {
		DB.withConnection { implicit c =>
      val id: Long = cat.id.getOrElse {
         SQL("select next value for cat_id_seq").as(scalar[Long].single)
       }

	      SQL("insert into category values ({id}, {name}, {order})").on(
          'id -> id,
	        'name -> cat.name,
	        'order -> cat.order
	      ).executeUpdate()

        Id(id)
	    }
	} 
}

object User {  
  val user = {
    get[Pk[Long]]("id") ~
	  get[String]("email") ~ 
	  get[String]("name") ~
	  get[String]("password") ~
	  get[Int]("role") map {
	    case id~email~name~password~role => User(id, email, name, password, if(0<=role && role<Role.maxId) Role(role) else Unprivileged)
	  }
  }
  
  def load(id: Long) : Option[User] = {
    DB.withConnection { implicit c =>
      SQL("select * from user where id = {id}").on('id -> id).as(user.singleOpt)
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
  
  def create(user: User) : Pk[Long] = {
    DB.withConnection { implicit c =>
      val id: Long = user.id.getOrElse {
         SQL("select next value for user_id_seq").as(scalar[Long].single)
       }

      SQL("insert into user values ({id}, {email}, {name}, {password}, {role})").on(
        'id -> id,
        'email -> user.email,
        'name -> user.name,
        'password -> user.password,
        'role -> user.role.id
      ).executeUpdate()

      Id(id)
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
    get[Long]("user_id") map {
      case id~stmt_id~content~date~user_id => Entry(id, stmt_id, content, date, User.load(user_id).get)
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
             {id}, {stmt_id}, {content}, {date}, {user_id}
           )
         """
       ).on(
         'id -> id,
         'stmt_id -> stmt_id,
         'content -> e.content.toString,
         'date -> e.date,
         'user_id -> e.user.id
       ).executeUpdate()
              
       Id(id)
     } 
  }
}

object Statement {  
  val stmt = {
	  get[Pk[Long]]("id") ~ 
	  get[String]("title")  ~
    get[Pk[Long]]("category.id") ~
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
             {id}, {title}, {cat_id}, {rating}
           )
         """
       ).on(
         'id -> id,
         'title -> stmt.title,
         'cat_id -> stmt.category.id,
         'rating -> stmt.rating.id
       ).executeUpdate()
       Id(id)
     }
    if(id.isDefined) stmt.entries.foreach{ entry => Entry.create(id.get, entry) }
    id
  }
}
