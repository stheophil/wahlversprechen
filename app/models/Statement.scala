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
import play.api.Logger
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
	def all(): Map[Author, List[Statement]] = {
		queryStatements().groupBy( _.author )
	}
	
	def load(id: Long): Option[Statement] = {
		queryStatements("statement.id = {id}", List('id -> id)).headOption
	}

	def loadAll(id: Long): List[Statement] = {
		queryStatements("statement.id = {id} OR statement.merged_id = {id}", List('id -> id))
	}

	def withEntries(stmt: Statement) : Statement = {
		stmt.copy(entries = Entry.loadByStatement(stmt.id))
	}

	def find(searchQuery: String) : Map[Author, List[Statement]] =  {
		val query = selectClause + ", ts_rank_cd(statement.textsearchable, to_tsquery({query})) AS rank " + 
			fromClause + 
			joinClause(false) +
			"WHERE statement.textsearchable @@ to_tsquery({query}) " +
			groupbyClause +
			"order by rank DESC"

		Logger.debug(query)

		DB.withConnection( implicit c => 
			SQL(query).on('query -> searchQuery).as(stmt*)
		).groupBy( _.author )
	}


	def byEntryDate(oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		filter(author = oauthor, limit = olimit, withEntriesOnly = true)
	}

	def byImportantTag(oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		filter(importantTagOnly = true, author = oauthor, limit = olimit)
	}

	def byTag(tag: String, oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		filter(tag = Some(tag), author = oauthor, limit = olimit)
	}

	def byCategory(category: String, oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		filter(category = Some(category), author = oauthor, limit = olimit)
	}

	def byRating(rating: Rating, oauthor: Option[Author], olimit: Option[Int]) : List[Statement] = {
		filter(rating = Some(rating), author = oauthor, limit = olimit)
	}

	def filter(category: Option[String] = None, author: Option[Author] = None, rating: Option[Rating] = None, importantTagOnly : Boolean = false, tag : Option[String] = None, limit: Option[Int] = None, withEntriesOnly : Boolean = false) : List[Statement] = {	
		val params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
		val conditions = new SQLWhereClause

		if(category.isDefined) {
			params += ('category -> category.get)
			conditions += ("category.name = {category}")
		}
		if(author.isDefined) {
			params += ('author_id -> author.get.id)
			conditions += ("author.id = {author_id}")
		}
		if(rating.isDefined) {
			params += ('rating -> rating.get.id)
			conditions += ("(statement.rating = {rating} OR statement2.rating = {rating})")
		}
		if(importantTagOnly || tag.isDefined) {
			val conditionsTag = new SQLWhereClause 
			if(importantTagOnly) conditionsTag += ("tag.important = TRUE")
			if(tag.isDefined) {
				params += ('tag -> tag.get)
				conditionsTag += ("tag.name = {tag}")
			}
			conditions += ("""statement.id IN (SELECT statement.id 
				FROM statement 
				JOIN statement_tags ON statement_tags.stmt_id = statement.id
				JOIN tag ON tag.id = statement_tags.tag_id """ + conditionsTag + ")")
		}

		val querySql = selectClause + fromClause + joinClause(withEntriesOnly) +
			conditions +
			groupbyClause + 
			(if(withEntriesOnly) {
				"ORDER BY latestEntry DESC "
			} else orderbyClause) +
			(if(limit.isDefined) {
				if(limit.isDefined)  params += ('limit -> limit.get)
				"limit {limit}"
			} else "")

		DB.withConnection({ implicit c =>
			Logger.debug("filter() querySql = " + querySql)
			SQL(querySql).on(params:_*).as(stmt*)
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

	// TODO: This is a poor man's database abstraction layer.
	// Replace with Slick or sth similar as soon as possible
	implicit def rowToSeqString: Column[Seq[String]] = Column.nonNull { (value, meta) =>
	  val MetaDataItem(qualified, nullable, clazz) = meta
	  value match {
	    case arr: java.sql.Array => Right(arr.getArray.asInstanceOf[Array[String]].toSeq)
	    case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Seq[String] for column " + qualified))
	  } 
	}

	implicit def rowToSeqInt: Column[Seq[Int]] = Column.nonNull { (value, meta) =>
	  val MetaDataItem(qualified, nullable, clazz) = meta
	  value match {
	    case arr: java.sql.Array => Right(arr.getArray.asInstanceOf[Array[Integer]].map(_.intValue).toSeq)
	    case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Seq[Int] for column " + qualified))
	  } 
	}

	implicit def rowToSeqBoolean: Column[Seq[Boolean]] = Column.nonNull { (value, meta) =>
	  val MetaDataItem(qualified, nullable, clazz) = meta
	  value match {
	    case arr: java.sql.Array => Right(arr.getArray.asInstanceOf[Array[java.lang.Boolean]].map(_.booleanValue).toSeq)
	    case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Seq[Int] for column " + qualified))
	  } 
	}

	private val stmt = {
			get[Long]("statement.id") ~
			get[String]("statement.title") ~
			get[Option[String]]("statement.quote") ~
			get[Option[String]]("statement.quote_src") ~
			get[Option[Date]]("latestEntry") ~
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
			get[Long]("category.ordering") ~
			get[Seq[Int]]("tag_id") ~
			get[Seq[String]]("tag_name") ~
			get[Seq[Boolean]]("tag_important") map {
				case id ~ title ~ quote ~ quote_src ~ latestEntry ~ rating ~ rated ~ merged_id  ~ merged_rating ~ merged_rated ~ 
					author_id ~ author_name ~ author_order ~ author_rated ~ author_color ~ author_background ~ 
				 	category_id ~ category_name ~ category_order ~ tag_id ~ tag_name ~ tag_important => 
				Statement(id, title, 
					Author(author_id, author_name, author_order, author_rated, author_color, author_background),
					Category(category_id, category_name, category_order),
					quote, quote_src, 
					List[Entry](), 
					latestEntry,
					(tag_id, tag_name, tag_important).zipped.map( (id, name, important) => Tag(id, name, important) ).toList.sortBy(_.name),
					(if(merged_id.isDefined && !rating.isDefined) merged_rating else rating) map { r => if (0 <= r && r < Rating.maxId) Rating(r) else Unrated },
					(if(merged_id.isDefined && !rating.isDefined) merged_rated else rated),
					merged_id
				)
			} // Rating(rating) would throw java.util.NoSuchElementException
	}

	val selectClause = 
		"""SELECT statement.id, statement.title, statement.quote, statement.quote_src, MAX(entry.date) AS latestEntry, 
		statement.rating, statement.rated, statement.merged_id, statement2.rating AS merged_rating, statement2.rated AS merged_rated,
		category.id, category.name, category.ordering,
		author.id, author.name, author.ordering, author.rated, author.color, author.background,
		ARRAY_AGG(tag.id) AS tag_id, ARRAY_AGG(tag.name) AS tag_name, ARRAY_AGG(tag.important) AS tag_important """

	val fromClause = "FROM statement "

	// Huge join but takes only 70ms vs 20ms without aggregating the tags too on the Heroku instance	
	private def joinClause(withEntriesOnly: Boolean) : String = {
		"""JOIN category ON category.id=statement.cat_id
		JOIN author ON author.id=statement.author_id
		LEFT JOIN statement statement2 ON statement2.id = statement.merged_id
		LEFT JOIN statement_tags ON statement.id = statement_tags.stmt_id 
		JOIN tag on statement_tags.tag_id = tag.id """ + 
		(if(!withEntriesOnly) { "LEFT " } else "") + 
		"JOIN entry on statement.id = entry.stmt_id "
	}

	private val groupbyClause = "GROUP BY statement.id, category.id, author.id, statement2.id "
	private val orderbyClause = "ORDER BY author.ordering ASC, category.ordering ASC, statement.id ASC "

	private def queryStatements(whereClause : String = "", params: List[(Any, anorm.ParameterValue[_])] = List.empty[(Any, anorm.ParameterValue[_])]) : List[Statement] = {
		val querySql = selectClause + fromClause + joinClause(false) + 
			(if(whereClause.isEmpty) { "" } else { " WHERE " + whereClause + " "}) + 
			groupbyClause + 
			orderbyClause

		Logger.debug("queryStatements querySql = " + querySql)
		Logger.debug("queryStatements params = " + params)
		DB.withConnection({ implicit c =>
			SQL(querySql).on(params:_*).as(stmt*)
		})		
	}

	private class SQLWhereClause extends {
		val conditions = collection.mutable.ListBuffer[String]()
		def +=( condition: String ) {
			conditions += condition
		}

		override def toString() : String = {
			conditions.foldLeft("")( {
				case ("", cond) => "WHERE " + cond + " "
				case (prev, cond) => prev + " AND " + cond + " "
			})
		}
	}

	import play.api.libs.json._
	implicit val StatementToJson = new Writes[Statement] {
	  def writes(s: Statement): JsValue = {
	    Json.obj(
	    	"id" -> s.id,
	    	"title" -> s.title,
	    	"quote" -> s.quote,
	    	"quote_src" -> s.quote_src,
	    	"author" -> s.author.name,
	    	"category" -> s.category.name,
	    	"tags" -> s.tags.map( _.name ),
	    	"rating" -> s.rating.map( _.toString ).getOrElse("").toString,
	    	"linked_to" -> s.merged_id.map( _.toString ).getOrElse( "" ).toString 
	    )
	  }
	}
}
