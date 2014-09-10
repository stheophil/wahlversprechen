package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import java.util.{Calendar, Date}

object RelatedCategory extends Enumeration {
  type RelatedCategory = Value
  val Article = Value
}
import RelatedCategory._

/**
 * URL related to a [[Statement]], determined by a text matching algorithm
 * @param stmt_id the statement
 * @param title the title, e.g. the web site title
 * @param url the URL
 * @param confidence a value for the confidence of the matching algorithm
 * @param lastseen when was this URL last seen, e.g., in a RSS feed
 * @param urltype type of this url, always [[RelatedCategory.Article]] at the moment
 */
case class RelatedUrl(id: Long, stmt_id: Long, title: String, url: String, confidence: Double, lastseen: Date, urltype: RelatedCategory)
case class RelatedUrlGroup(stmt_id: Long, stmt_title: String, category: String, articles: List[RelatedUrl])

object RelatedUrl {
  val relatedurl = {
    get[Long]("id") ~
    get[Long]("stmt_id") ~
    get[String]("title") ~
    get[String]("url") ~
    get[Double]("confidence") ~
    get[Date]("lastseen") ~
    get[Int]("urltype") map {
      case id ~ stmt_id ~ title ~ url ~ confidence ~ lastseen ~ urltype =>
        RelatedUrl(id, stmt_id, title, url, confidence, lastseen, RelatedCategory( Math.min(RelatedCategory.maxId, Math.max(urltype, 0)) ))
    }
  }

  def loadByUrl(stmt_id: Long, url: String) : Option[RelatedUrl] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM relatedurl WHERE stmt_id = {stmt_id} AND url = {url}").
        on('stmt_id -> stmt_id, 'url -> url).as(relatedurl.singleOpt)
    }
  }

  def load(stmt_id: Long) : List[RelatedUrl] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM relatedurl WHERE stmt_id = {stmt_id} ORDER BY confidence DESC").on('stmt_id -> stmt_id).as(relatedurl*)
    }
  }

  def load(stmt_id: Long, limit: Option[Int] = None, offset: Option[Int] = None ) : List[RelatedUrl] = {
    DB.withConnection { implicit c =>
      val query = "SELECT * FROM relatedurl WHERE stmt_id = {stmt_id} ORDER BY lastseen DESC OFFSET {offset}"
      val queryLimit = query + " LIMIT {limit}"
      limit match {
        case None => SQL(query).on('stmt_id -> stmt_id, 'offset -> offset.getOrElse(0)).as(relatedurl*)
        case Some(l) => SQL(queryLimit).on('stmt_id -> stmt_id, 'offset -> offset.getOrElse(0), 'limit -> l).as(relatedurl*)
      }
    }
  }

  def loadRecent(from: Date, to: Option[Date]) : List[RelatedUrl] = {
    DB.withConnection { implicit c =>
      val query = "SELECT * FROM relatedurl WHERE {from} <= relatedurl.lastseen" 
      val orderQuery = " ORDER BY lastseen ASC"
      to match {
        case None => SQL(query+orderQuery).on('from -> from).as(relatedurl*)
        case Some(t) => 
          SQL(query + " AND relatedurl.lastseen < {to}" + orderQuery).
            on('from -> from, 'to -> t).
            as(relatedurl *)
      }
    }
  }

  def loadRecentGroups(daysSince: Int, limit: Option[Int]) : List[RelatedUrlGroup] = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, - daysSince)

    val relatedUrlWithStatement = long("stmt_id")~str("stmt_title") ~ str("category") ~ relatedurl map {
      case stmt_id~stmt_title~category~relatedurl => (stmt_id, stmt_title, category, relatedurl)
    }

    val listRelatedUrlWithStatement = 
      DB.withConnection { implicit c =>
        val query = """SELECT statement.id AS stmt_id, statement.title AS stmt_title, 
        category.name AS category, relatedurl.*
        FROM relatedurl 
        JOIN statement ON statement.id = relatedurl.stmt_id
        JOIN category on statement.cat_id = category.id
        WHERE relatedurl.lastseen >= {day} ORDER BY confidence DESC"""
        val queryLimit = query + " LIMIT {limit}"
        limit match {
          case None => SQL(query).on('day -> cal.getTime).as(relatedUrlWithStatement*)
          case Some(l) => SQL(queryLimit).on('day -> cal.getTime, 'limit -> l).as(relatedUrlWithStatement*)
        }
      }

    listRelatedUrlWithStatement.groupBy(_._1).values.map(
      list => RelatedUrlGroup(list.head._1, list.head._2, list.head._3, list.map(_._4).sortBy(_.confidence)(Ordering[Double].reverse))
    ).toList.sortBy(_.articles.head.confidence)(Ordering[Double].reverse)
  }

  def create(stmt_id: Long, title: String, url: String, confidence: Double, date: Date, urltype: RelatedCategory): RelatedUrl = {
    DB.withConnection { implicit c =>
      val id = SQL("INSERT INTO relatedurl VALUES (DEFAULT, {stmt_id}, {title}, {url}, {confidence}, {date}, {urltype}) RETURNING id").on(
        'stmt_id -> stmt_id , 'title -> title , 'url -> url, 'confidence -> confidence, 'date -> date, 'urltype -> urltype.id
      ).as(scalar[Long].single)
      RelatedUrl(id, stmt_id, title, url, confidence, date, urltype)
    }
  }

  def update(id: Long, date: Date) : Boolean = {
    DB.withConnection{ implicit c =>
      0 < SQL("UPDATE relatedurl SET lastseen = {date} WHERE id = {id}").on('date -> date, 'id -> id).executeUpdate()
    }
  }

  import play.api.libs.json._
  implicit val RelatedUrlToJson = new Writes[RelatedUrl] {
    def writes(s: RelatedUrl): JsValue = {
      Json.obj(
        "id" -> s.id,
        "stmt_id" -> s.stmt_id,
        "title" -> s.title,
        "url" -> s.url,
        "confidence" -> s.confidence,
        "lastseen" -> Formatter.formatISO8601(s.lastseen)
      )
    }
  }
}
