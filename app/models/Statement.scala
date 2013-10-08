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
case class Author(id: Long, name: String, order: Long, rated: Boolean, color: String, background: String)
case class Tag(id: Long, name: String)
case class User(id: Long, email: String, name: String, password: String, salt: String, role: Role)
case class Entry(id: Long, stmt_id: Long, content: String, date: Date, user: User)
case class Statement(id: Long, title: String, author: Author, category: Category, 
	quote: Option[String], quote_src: Option[String], 
	entries: List[Entry], latestEntry: Option[Date], 
	tags: List[Tag], 
	rating: Option[Rating], rated: Option[Date], 
	merged_id: Option[Long])

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

	def loadAll(implicit connection: java.sql.Connection): List[Category] = {
		SQL("select * from category order by ordering").as(category*)
	}
}

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

	def load(name : String): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("select * from author where name = {name}").on('name -> name).as(author.singleOpt)
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

object Tag {
	val tag = {
		get[Long]("id") ~
		get[String]("name") map {
			case id ~ name => Tag(id, name)
		}
	}

	def create(name: String): Tag = {
		DB.withConnection { implicit c => create(name) }
	}

	def create(implicit connection: java.sql.Connection, name: String): Tag = {
		val id: Long = SQL("select nextval('tag_id_seq')").as(scalar[Long].single)

		SQL("insert into tag values ({id}, {name})").on('id -> id, 'name -> name).executeUpdate()
		Tag(id, name)
	}

	def loadByStatement(stmt_id: Long): List[Tag] = {
		DB.withConnection { implicit c =>
			SQL("""select id, name from tag 
			join statement_tags on statement_tags.tag_id=id 
			where statement_tags.stmt_id = {stmt_id} 
			order by tag.name""").on('stmt_id -> stmt_id).as(tag*)
		}
	}

	def load(name: String): Option[Tag] = {
		DB.withConnection { implicit c =>
			SQL("select id, name from tag where tag.name={name}").on('name -> name).as(tag.singleOpt)
		}
	}

	def loadAll(implicit connection: java.sql.Connection): List[Tag] = {
			SQL("select id, name from tag").as(tag*)
	}

	def add(implicit connection: java.sql.Connection, stmt: Statement, tag: Tag) {
			SQL("insert into statement_tags values ({tag_id}, {stmt_id})").on(
				'tag_id -> tag.id,
				'stmt_id -> stmt.id
			).executeUpdate
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

object Statement {
	/* Invariants:
		1. Statement with rated Author
			always has rating and no merged_id
		2. Statement with non-rated Author
			may have either rating or merged_id
			in case data is inconsistent, merged_id has precedence
	*/
	val stmt = {
			get[Long]("statement.id") ~
			get[String]("statement.title") ~
			get[Option[String]]("statement.quote") ~
			get[Option[String]]("statement.quote_src") ~
			get[Option[Date]]("statement.latestEntry") ~
			get[Option[Int]]("statement.rating") ~
			get[Option[Date]]("statement.rated") ~
			get[Option[Long]]("statement.merged_id") ~
			get[Option[Int]]("merged_rating") ~
			get[Option[Date]]("merged_rated") ~
			get[Long]("author.id") ~
			get[String]("author.name") ~
			get[Long]("author.ordering") ~
			get[Boolean]("author.rated") ~
			get[String]("author.color") ~
			get[String]("author.background") ~
			get[Long]("category.id") ~
			get[String]("category.name") ~
			get[Long]("category.ordering")  map {
				case id ~ title ~ quote ~ quote_src ~ latestEntry ~ rating ~ rated ~ merged_id  ~ merged_rating ~ merged_rated ~ 
					author_id ~ author_name ~ author_order ~ author_rated ~ author_color ~ author_background ~ 
				 	category_id ~ category_name ~ category_order => 
				Statement(id, title, 
					Author(author_id, author_name, author_order, author_rated, author_color, author_background),
					Category(category_id, category_name, category_order),
					quote, quote_src, 
					List[Entry](), 
					latestEntry,
					List[Tag](),
					(if(merged_id.isDefined) merged_rating else rating) map { r => if (0 <= r && r < Rating.maxId) Rating(r) else Unrated },
					(if(merged_id.isDefined) merged_rated else rated),
					merged_id
				)
			} // Rating(rating) would throw java.util.NoSuchElementException
	}

	val query = """SELECT statement.id, statement.title, statement.quote, statement.quote_src, statement.latestEntry, 
		statement.rating, statement.rated, statement.merged_id, statement2.rating as merged_rating, statement2.rated as merged_rated,
		category.id, category.name, category.ordering,
		author.id, author.name, author.ordering, author.rated, author.color, author.background
		from statement 
		join category on category.id=statement.cat_id
		join author on author.id=statement.author_id
		left join statement statement2 on statement2.id = statement.merged_id"""

	val queryOrdering = " order by author.ordering ASC, category.ordering ASC, statement.id ASC"
	
	def all(): Map[Author, List[Statement]] = {
		DB.withConnection({ implicit c =>
			SQL(query+queryOrdering).as(stmt*)
		}).groupBy( _.author )
	}

	def find(searchQuery: String) : Map[Author, List[Statement]] =  {
		val queryLike = query + """ 
		join statement_tags on statement_tags.stmt_id=statement.id 
		join tag on statement_tags.tag_id = tag.id
		where LOWER(statement.title) like {query} or LOWER(category.name) like {query} or LOWER(statement.quote) like {query} or 
		LOWER(tag.name) like {query}
		group by statement.id, category.id, author.id, statement2.id""" + queryOrdering;

		val queryString = "%" + searchQuery.toLowerCase + "%"
		DB.withConnection({ implicit c =>
			SQL(queryLike).on('query -> queryString).as(stmt*)
		}).groupBy( _.author )
	}

	def byEntryDate(oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		val queryLatest = query +
		" where statement.latestEntry IS NOT NULL "
		(if(oauthor.isDefined) " and author.id = {author_id} " else "") +
		"order by statement.latestEntry DESC " +
		(if(olimit.isDefined) "limit {limit}" else "")

		DB.withConnection({ implicit c =>			
			var params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
			if(olimit.isDefined)  params += ('limit -> olimit.get)
			if(oauthor.isDefined) params += ('author_id -> oauthor.get.id)

			SQL(queryLatest).on(params:_*).as(stmt*)
		})
	}

	def byTag(tag: String, oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		val queryWithTag = query + 
			" join statement_tags on statement_tags.stmt_id = statement.id " +
			" join tag on statement_tags.tag_id = tag.id and tag.name = {tagname} " +
			(if(oauthor.isDefined) "where author.id = {author_id} " else "") +
			" order by category.ordering ASC, statement.id ASC " +
			(if(olimit.isDefined) "limit {limit}" else "")

		DB.withConnection({ implicit c =>
			var params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
			params += ('tagname -> tag)
			if(olimit.isDefined)  params += ('limit -> olimit.get)
			if(oauthor.isDefined) params += ('author_id -> oauthor.get.id)			

			SQL(queryWithTag).on(params:_*).as(stmt*)
		})
	}

	def byCategory(category: String, oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		val queryWithTag = query + 
			" where category.name = {name} " +
			(if(oauthor.isDefined) "and author.id = {author_id} " else "") +
			"order by category.ordering ASC, statement.id ASC " +
			(if(olimit.isDefined) "limit {limit}" else "")

		DB.withConnection({ implicit c =>
			var params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
			params += ('name -> category)
			if(olimit.isDefined)  params += ('limit -> olimit.get)
			if(oauthor.isDefined) params += ('author_id -> oauthor.get.id)			

			SQL(queryWithTag).on(params:_*).as(stmt*)
		})
	}

	def byRating(rating: Rating, oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		val queryWithTag = query + 
			" where statement.rating = {rating} or statement2.rating = {rating} " +
			(if(oauthor.isDefined) "and author.id = {author_id} " else "") +
			"order by category.ordering ASC, statement.id ASC " +
			(if(olimit.isDefined) "limit {limit}" else "")

		DB.withConnection({ implicit c =>
			var params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
			params += ('rating -> rating.id)
			params += ('rating -> rating.id)
			if(olimit.isDefined)  params += ('limit -> olimit.get)
			if(oauthor.isDefined) params += ('author_id -> oauthor.get.id)			

			SQL(queryWithTag).on(params:_*).as(stmt*)
		})
	}

	def loadEntriesTags(stmt: Statement) : Statement = {
		Statement(stmt.id, stmt.title, stmt.author, stmt.category, stmt.quote, stmt.quote_src, 
				Entry.loadByStatement(stmt.id), stmt.latestEntry, Tag.loadByStatement(stmt.id), stmt.rating, stmt.rated, stmt.merged_id)
	}
	
	def load(id: Long): Option[Statement] = {
		// TODO: Create stmt with entries in the first place
		DB.withConnection({ implicit c =>
			SQL(query+" where statement.id = {id}").on('id -> id).as(stmt.singleOpt)
		})
	}

	def loadAll(id: Long): List[Statement] = {
		// TODO: Create stmt with entries in the first place
		DB.withConnection({ implicit c =>
			SQL(query+" where statement.id = {id} or statement.merged_id = {id} ").on('id -> id).as(stmt*)
		})
	}

	def countRatings(author: Author) : (Int, Map[Rating,Int]) = {
		val ratingCount = {
			get[Option[Int]]("rating") ~
			get[Long]("rating_count") map {
				case rating ~ count  => {
					( {if(rating.isDefined && 0 <= rating.get && rating.get < Rating.maxId) Rating(rating.get) else Unrated} -> count.toInt )
				}
			}
		}

		DB.withConnection({ implicit c => 
			( 				
				SQL("select COUNT(*) from statement where author_id = {id}").on('id -> author.id).as(scalar[Long].single).toInt,
				SQL("select rating, COUNT(rating) as rating_count from statement where author_id = {id} group by rating").
					on('id -> author.id).
					as(ratingCount*).
					toMap[Rating, Int]
			)
		})
	}

	def create(title: String, author: Author, cat: Category, quote: Option[String], quote_src: Option[String], rating: Option[Rating], merged_id: Option[Long]): Statement = {
		DB.withTransaction { implicit c => Statement.create(c, title, author, cat, quote, quote_src, rating, merged_id) }
	}

	def create(implicit connection: java.sql.Connection, title: String, author: Author, cat: Category, quote: Option[String], quote_src: Option[String], rating: Option[Rating], merged_id: Option[Long]): Statement = {
		
		require(author.rated == rating.isDefined)
		require(!merged_id.isDefined || !author.rated)

		// Get the project id
		val id: Long = SQL("select nextval('stmt_id_seq')").as(scalar[Long].single)
		val rated = rating map { r => new Date() };
		// Insert the project
		SQL("insert into statement values ({id}, {title}, {author_id}, {cat_id}, {quote}, {quote_src}, NULL, {rating}, {rated}, {merged_id})").on(
				'id -> id,
				'title -> title,
				'author_id -> author.id,
				'cat_id -> cat.id,
				'quote -> quote,
				'quote_src -> quote_src, 
				'rating -> { rating map { _.id } },
				'rated -> rated,
				'merged_id -> merged_id).executeUpdate()

		Statement(id, title, author, cat, quote, quote_src, List[Entry](), None, List[Tag](), rating, rated, merged_id)
	}

	def rate(stmt_id: Int, rating: Int, date: Date) {
		// TODO rating not in models.Rating -> erase rating, assert author is not rated
		DB.withConnection { implicit c => 
			SQL("update statement set rating = {rating}, rated = {rated} where id = {stmt_id}").
				on('rating -> (if(0<=rating && rating<models.Rating.maxId) { Some(rating) } else { None }),
					'rated -> date,
					'stmt_id -> stmt_id 
				).executeUpdate
		}
	}
}
