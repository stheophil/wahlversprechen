package models

object Rating extends Enumeration {
	type Rating = Value
	val PromiseKept, Compromise, PromiseBroken, Stalled, InTheWorks, Unrated = Value
};
import Rating._

import java.util.Date

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current

case class Statement(id: Long, title: String, author: Author, category: Category, 
	quote: Option[String], quote_src: Option[String], 
	entries: List[Entry], latestEntry: Option[Date], 
	tags: List[Tag], 
	rating: Option[Rating], rated: Option[Date], 
	merged_id: Option[Long])

object Statement {
	/* Invariants:
		1. Statement with rated Author
			always has rating and no merged_id
		2. Statement with non-rated Author
			may have rating and merged_id:
			- if merged_id == null
				-> display rating (may be null)
			- else 
				if rating == null
					-> display rating of statement referred to by merged_id 
				else
					-> display rating
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
					(if(merged_id.isDefined && !rating.isDefined) merged_rating else rating) map { r => if (0 <= r && r < Rating.maxId) Rating(r) else Unrated },
					(if(merged_id.isDefined && !rating.isDefined) merged_rated else rated),
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
		" where statement.latestEntry IS NOT NULL " + 
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

	def byImportantTag(oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		val queryWithTag = query + 
			" join statement_tags on statement_tags.stmt_id = statement.id " +
			" join tag on statement_tags.tag_id = tag.id and tag.important = TRUE " +
			(if(oauthor.isDefined) "where author.id = {author_id} " else "") +
			" order by category.ordering ASC, statement.id ASC " +
			(if(olimit.isDefined) "limit {limit}" else "")

		DB.withConnection({ implicit c =>
			var params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
			if(olimit.isDefined)  params += ('limit -> olimit.get)
			if(oauthor.isDefined) params += ('author_id -> oauthor.get.id)			

			SQL(queryWithTag).on(params:_*).as(stmt*)
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
		
		require(author.rated || merged_id.isDefined || !rating.isDefined)

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

	def edit(implicit connection: java.sql.Connection, id: Long, title: String, cat: Category, quote: Option[String], quote_src: Option[String], rating: Option[Rating], merged_id: Option[Long]) {	
		val rated = rating map { r => new Date() };
		// Insert the project
		SQL("update statement set title={title}, cat_id={cat_id}, quote={quote}, quote_src= {quote_src}, rating={rating}, rated={rated}, merged_id={merged_id} where id = {id}").on(
				'id -> id,
				'title -> title,
				'cat_id -> cat.id,
				'quote -> quote,
				'quote_src -> quote_src, 
				'rating -> { rating map { _.id } },
				'rated -> rated,
				'merged_id -> merged_id).executeUpdate()
	}

	def delete(id: Long) {
		DB.withTransaction { implicit c => 
			Tag.eraseAll(c, id)
			Entry.deleteAll(c, id)
			SQL("UPDATE STATEMENT SET merged_id = NULL WHERE merged_id = {id} ").on('id -> id).executeUpdate
			SQL("DELETE FROM STATEMENT WHERE id = {id}").on('id -> id).executeUpdate
		}
	}

	def setTitle(stmt_id: Long, title: String) {
		DB.withConnection { implicit c => 
			SQL("update statement set title = {title} where id = {stmt_id}").
				on('title -> title, 'stmt_id -> stmt_id).executeUpdate
		}
	}

	def setQuote(stmt_id: Long, quote: String) {
		DB.withConnection { implicit c => 
			SQL("update statement set quote = {quote} where id = {stmt_id}").
				on('quote -> quote, 'stmt_id -> stmt_id).executeUpdate
		}
	}

	def setQuoteSrc(stmt_id: Long, quote_src: String) {
		DB.withConnection { implicit c => 
			SQL("update statement set quote_src = {quote_src} where id = {stmt_id}").
				on('quote_src -> quote_src, 'stmt_id -> stmt_id).executeUpdate
		}
	}

	def setRating(stmt_id: Long, rating: Int, date: Date) {
		// TODO rating not in models.Rating -> erase rating, assert author is not rated
		DB.withConnection { implicit c => 
			SQL("update statement set rating = {rating}, rated = {rated} where id = {stmt_id}").
				on('rating -> (if(0<=rating && rating<models.Rating.maxId) { Some(rating) } else { None }),
					'rated -> date,
					'stmt_id -> stmt_id 
				).executeUpdate
		}
	}

	def setMergedID(implicit connection: java.sql.Connection, stmt_id: Int, merged_id: Long) {
		// TODO assert author of merged_id is rated, author of stmt_id is not rated
		SQL("update statement set merged_id = {merged_id} where id = {stmt_id}").
				on('merged_id -> merged_id, 'stmt_id -> stmt_id).executeUpdate
	}
}
