package models

import java.util.Date

import collection.SortedSet
import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Logger
import play.api.Play.current
import models.Rating.Rating

object Rating extends Enumeration {
	type Rating = Value
	val PromiseKept, Compromise, PromiseBroken, Stalled, InTheWorks, Unrated = Value
}

/** A statement, e.g. a campaign promise, that can be rated. <br/>
 *
 * '''Invariants'''
 *
 *  1. If `author.top_level`, `rating` must be set and `linked_id` must not be set.
 *	   [[Author]]s form a two level hierarchy. Top-level authors and non-top-level authors. 
 * 	   Statements belonging to subordinate Authors may link to the statements of
 *	   top-level Authors.
 *  1. If `!author.top_level`, `rating` or `linked_id` may be set. If `rating` is set,
 *	   this rating will be displayed by all views. Otherwise, if `linked_id` is set,
 *	   the rating of the [[Statement]] referred to by `linked_id` will be displayed.
 *
 *  For performance reasons, Statements loaded from the database contain an empty 
 *  list of `entries` and `ratings` contains the latest rating only. 
 *  Load the complete Statement by calling `withEntriesAndRatings`.
 *	@param title the title as simple text (no Markdown)
 *	@param author who made this statement
 *	@param category a category, eg, "Foreign Affairs", "Commerce" etc
 *	@param quote an exact quote of this statement, eg, from an election program (supports Markdown syntax)
 *	@param quote_src the source of the quote (also supports Markdown)
 *	@param entries a list of updates to this statement
 *	@param latestEntry date when latest entry was written
 *	@param tags a set of tags for this statement, ordered by name
 *	@param rating the current rating
 *	@param rated time of last rating
 *	@param linked_id id of another this statement is linked to
 */
case class Statement(id: Long, title: String, author: Author, category: Category,
	quote: Option[String], quote_src: Option[String],
	entries: List[Entry], latestEntry: Option[Date],
	tags: SortedSet[Tag],
	ratings: List[(Rating, Date)],
	linked_id: Option[Long]) {

	def rating : Option[Rating] = ratings.headOption.map(_._1)
	def rated : Option[Date] = ratings.headOption.map(_._2)
}

object Statement {
	def all(): Map[Author, List[Statement]] = {
		DB.withConnection( implicit c => 
			SQL("SELECT * FROM full_statement").as(stmt*).groupBy( _.author )
		)
	}

	def load(id: Long): Option[Statement] = {
		DB.withConnection( implicit c => 
			SQL("SELECT * FROM full_statement where id = {id}").on('id -> id).as(stmt.singleOpt)
		)
	}

	def loadAll(id: Long): List[Statement] = {
		DB.withConnection( implicit c => 
			SQL("SELECT * FROM full_statement where id = {id} OR linked_id = {id}").on('id -> id).as(stmt*)
		)
	}

	def withEntriesAndRatings(stmt: Statement) : Statement = {
		val rating = {
			get[Int]("rating") ~
			get[Date]("rated") map {
				case rating ~ rated => (clampRating(rating), rated)
			}
		}

		val ratings = DB.withConnection( implicit c =>
			SQL("""SELECT rating, rated FROM statement_rating 
				WHERE stmt_id = {id} 
				ORDER BY rated DESC""").on('id -> stmt.id).as(rating*)
		)
		stmt.copy(entries = Entry.loadByStatement(stmt.id), ratings = ratings)
	}

	def find(searchQuery: String) : Map[Author, List[Statement]] =  {
		DB.withConnection( implicit c =>
			SQL("""SELECT *, ts_rank_cd(textsearchable, plainto_tsquery({query}), 1) AS rank 
				FROM full_statement 
				WHERE textsearchable @@ plainto_tsquery({query}) 
				ORDER BY rank DESC""").on('query -> searchQuery).as(stmt*)
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
		class SQLWhereClause {
			val conditions = collection.mutable.ListBuffer[String]()
			def +=( condition: String ) {
				conditions += condition
			}

			override def toString() : String = {
				conditions.foldLeft("")( {
					case ("", cond) => " WHERE " + cond + " "
					case (prev, cond) => prev + " AND " + cond + " "
				})
			}
		}

		val params = collection.mutable.ListBuffer[(Any, anorm.ParameterValue[_])]()
		val conditions = new SQLWhereClause

		if(category.isDefined) {
			params += ('category -> category.get)
			conditions += ("cat_name = {category}")
		}
		if(author.isDefined) {
			params += ('author_id -> author.get.id)
			conditions += ("author_id = {author_id}")
		}
		if(rating.isDefined) {
			params += ('rating -> rating.get.id)
			conditions += ("(rating = {rating} OR linked_rating = {rating})")
		}
		if(importantTagOnly || tag.isDefined) {
			val conditionsTag = new SQLWhereClause
			if(importantTagOnly) conditionsTag += ("tag.important = TRUE")
			if(tag.isDefined) {
				params += ('tag -> tag.get)
				conditionsTag += ("tag.name = {tag}")
			}
			conditions += ("""id IN (SELECT statement.id
				FROM statement 
				JOIN statement_tags ON statement_tags.stmt_id = statement.id
				JOIN tag ON tag.id = statement_tags.tag_id """ + conditionsTag + ")")
		}
		if(withEntriesOnly) {
			conditions += "latestEntry IS NOT NULL"
		}

		val limitClause = limit.map(" LIMIT " + _).getOrElse("") 
		val orderByClause = if(withEntriesOnly) { " ORDER BY latestEntry DESC "	} else { "" }

		DB.withConnection( implicit c =>
			SQL("SELECT * from full_statement" + conditions + orderByClause + limitClause).on(params:_*).as(stmt*)
		)
	}

	def countRatings(author: Author) : (Int, Map[Rating,Int]) = {
		val ratingCount = {
			get[Option[Int]]("rating") ~
			get[Long]("rating_count") map {
				case rating ~ count  => {
					( {if(rating.isDefined && 0 <= rating.get && rating.get < Rating.maxId) Rating(rating.get) else Rating.Unrated} -> count.toInt )
				}
			}
		}

		DB.withConnection({ implicit c =>
			(
				SQL("SELECT COUNT(*) FROM statement WHERE author_id = {id}").on('id -> author.id).as(scalar[Long].single).toInt,
				SQL("SELECT rating, COUNT(rating) AS rating_count FROM statement WHERE author_id = {id} GROUP BY rating").
					on('id -> author.id).
					as(ratingCount*).
					toMap[Rating, Int]
			)
		})
	}

	def create(title: String, author: Author, cat: Category, quote: Option[String], quote_src: Option[String]): Statement = {
		DB.withConnection { implicit c => Statement.create(c, title, author, cat, quote, quote_src) }
	}

	def create(implicit connection: java.sql.Connection, title: String, author: Author, cat: Category, quote: Option[String], quote_src: Option[String]): Statement = {
		val ratings = author.top_level match {
			case true => List((Rating.Unrated, new Date()))
			case false => List.empty[(Rating, Date)]
		}
		val id = SQL("INSERT INTO statement VALUES (DEFAULT, {title}, {author_id}, {cat_id}, {quote}, {quote_src}, {rating}, {rated}) RETURNING id").on(
				'title -> title, 
				'author_id -> author.id,
				'cat_id -> cat.id,
				'quote -> quote,
				'quote_src -> quote_src,
				'rating -> ratings.headOption.map( _._1.id ),
				'rated -> ratings.headOption.map( _._2 )
		).as(scalar[Long].single)

		Statement(id, title, author, cat, quote, quote_src, List.empty[Entry], None, SortedSet.empty[Tag], ratings, None)
	}

	def edit(id: Long, title: String, cat: Category, quote: Option[String], quote_src: Option[String]) : Boolean = {
		DB.withConnection( implicit c => Statement.edit(c, id, title, cat, quote, quote_src))
	}

	def edit(implicit connection: java.sql.Connection, id: Long, title: String, cat: Category, quote: Option[String], quote_src: Option[String]) : Boolean = {
		0 < SQL("UPDATE statement SET title={title}, cat_id={cat_id}, quote={quote}, quote_src= {quote_src} WHERE id = {id}").on(
				'id -> id,
				'title -> title,
				'cat_id -> cat.id,
				'quote -> quote,
				'quote_src -> quote_src).executeUpdate()
	}

	def delete(id: Long) : Boolean = {
		DB.withTransaction { implicit c =>
			0 < SQL("DELETE FROM statement WHERE id = {id}").on('id -> id).executeUpdate
		}
	}

	def setTitle(implicit connection: java.sql.Connection, stmt_id: Long, title: String) : Boolean = {
		0 < SQL("UPDATE statement SET title = {title} WHERE id = {stmt_id}").
			on('title -> title, 'stmt_id -> stmt_id).executeUpdate
	}

	def setQuote(implicit connection: java.sql.Connection, stmt_id: Long, quote: String) : Boolean = {
		0 < SQL("UPDATE statement SET quote = {quote} WHERE id = {stmt_id}").
			on('quote -> quote, 'stmt_id -> stmt_id).executeUpdate
	}

	def setQuoteSrc(implicit connection: java.sql.Connection, stmt_id: Long, quote_src: String) : Boolean = {
		0 < SQL("UPDATE statement SET quote_src = {quote_src} WHERE id = {stmt_id}").
			on('quote_src -> quote_src, 'stmt_id -> stmt_id).executeUpdate
	}

	def setRating(stmt_id: Long, rating: Rating) : Boolean = {
		DB.withTransaction( implicit c => setRating(c, stmt_id, rating) )
	}

	def setRating(implicit connection: java.sql.Connection, stmt_id: Long, rating: Rating) : Boolean = {
		val date = new java.util.Date()
		if( 0 < SQL("UPDATE statement SET rating = {rating}, rated = {rated} WHERE id = {stmt_id}").
			on('rating -> rating.id,
				'rated -> date,
				'stmt_id -> stmt_id
			).executeUpdate ) {

			// This table only stores the history for visualizing it
			// Using it as a normalized data store for the ratings was too complicated
			// Getting the current rating for all statements already requires a SELECT +
			// INNER JOIN on statement_rating,
			// see http://stackoverflow.com/questions/586781/postgresql-fetch-the-row-which-has-the-max-value-for-a-column
			SQL("INSERT INTO statement_rating VALUES ({stmt_id}, {rating}, {rated})").
				on('rating -> rating.id,
					'rated -> date,
					'stmt_id -> stmt_id
			).executeUpdate

			true
		} else {
			false
		}
	}

	def setLinkedID(stmt_id: Long, linked_id: Long) : Boolean = {
		DB.withConnection( implicit c => setLinkedID(c, stmt_id, linked_id) )
	}

	def setLinkedID(implicit connection: java.sql.Connection, stmt_id: Long, linked_id: Long) : Boolean = {
		0 < SQL("""WITH top_level_stmt AS
			(SELECT statement.id as stmt_id FROM statement, author WHERE statement.author_id = author.id AND author.top_level), 
				second_level_stmt AS 
			(SELECT statement.id AS stmt_id FROM statement, author WHERE statement.author_id = author.id AND NOT author.top_level) 
				UPDATE statement SET linked_id = {linked_id} 
				WHERE statement.id = {stmt_id} 
					AND {stmt_id} IN (SELECT stmt_id FROM second_level_stmt) 
					AND {linked_id} IN (SELECT stmt_id FROM top_level_stmt)
			""").on('linked_id -> linked_id, 'stmt_id -> stmt_id).executeUpdate
	}

	def rowToSeq[JavaType, ScalaType](f : (JavaType) => ScalaType) : Column[Seq[ScalaType]] = Column.nonNull { (value, meta) =>
	  val MetaDataItem(qualified, nullable, clazz) = meta
	  value match {
	    case arrSQL: java.sql.Array => {
	    	val arr = arrSQL.getArray.asInstanceOf[Array[JavaType]]
	    	if(arr == null) {
	    		Right(Seq.empty[ScalaType])
	    	} else {
	    		Right(arr.filter(_ != null).map(f).toSeq)
	    	}
	    }
	    case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Seq[T] for column " + qualified))
	  }
	}

	implicit def rowToSeqString: Column[Seq[String]] = rowToSeq[String, String](s => s)
	implicit def rowToSeqInt: Column[Seq[Int]] = rowToSeq[Integer, Int](_.intValue)
	implicit def rowToSeqBoolean: Column[Seq[Boolean]] = rowToSeq[java.lang.Boolean, Boolean](_.booleanValue)
	private def clampRating(r: Int) = if (0 <= r && r < Rating.maxId) Rating(r) else Rating.Unrated

	private val stmt = {
			get[Long]("id") ~
			get[String]("title") ~
			get[Option[String]]("quote") ~
			get[Option[String]]("quote_src") ~
			get[Option[Date]]("latestEntry") ~
			get[Option[Int]]("rating") ~
			get[Option[Date]]("rated") ~
			get[Option[Long]]("linked_id") ~
			get[Option[Int]]("linked_rating") ~
			get[Option[Date]]("linked_rated") ~
			get[Long]("author_id") ~
			get[String]("author_name") ~
			get[Long]("author_ordering") ~
			get[Boolean]("author_top_level") ~
			get[String]("author_color") ~
			get[String]("author_background") ~
			get[Long]("cat_id") ~
			get[String]("cat_name") ~
			get[Long]("cat_ordering") ~
			get[Seq[Int]]("tag_id") ~
			get[Seq[String]]("tag_name") ~
			get[Seq[Boolean]]("tag_important") map {
				case id ~ title ~ quote ~ quote_src ~ latestEntry ~ rating ~ rated ~ linked_id  ~ linked_rating ~ linked_rated ~
					author_id ~ author_name ~ author_order ~ author_top_level ~ author_color ~ author_background ~
				 	cat_id ~ cat_name ~ cat_ordering ~ tag_id ~ tag_name ~ tag_important =>
				Statement(id, title,
					Author(author_id, author_name, author_order, author_top_level, author_color, author_background),
					Category(cat_id, cat_name, cat_ordering),
					quote, quote_src,
					List[Entry](),
					latestEntry,
					(tag_id, tag_name, tag_important).zipped.map( (id, name, important) => Tag(id, name, important) ).to[SortedSet],
					(rating, rated, linked_rating, linked_rated) match {
						// List of all past ratings loaded on demand only
						case (Some(r), Some(date), _, _) => List((clampRating(r), date))
						case (_, _, Some(r), Some(date)) => List((clampRating(r), date))
						case _ => List.empty[(Rating, Date)]
					},
					linked_id
				)
			} 
	}

	import play.api.libs.json._
	implicit val EntryToJson = new Writes[Entry] {
	  def writes(e: Entry): JsValue = {
	    Json.obj(
	    	"id" -> e.id,
	    	"content" -> e.content,
	    	"date" -> Formatter.formatRFC822(e.date),
	    	"user" -> e.user.name
	    )
	  }
	}

	implicit val RatingToJson = new Writes[(Rating, Date)] {
	  def writes(r: (Rating, Date)): JsValue = {
	  	Json.obj( 
	  		"rating" -> r._1.toString,
	  		"date" -> Formatter.formatRFC822(r._2) 
	  	)
	  }
	}

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
	    	"ratings" -> s.ratings,
	    	"entries" -> s.entries,
	    	"linked_to" -> s.linked_id.map( _.toString ).getOrElse( "" ).toString
	    )
	  }
	}
}
