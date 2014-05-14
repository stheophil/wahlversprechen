package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import java.util.Date

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
      SQL("SELECT * FROM relatedurl WHERE stmt_id = {stmt_id}").on('stmt_id -> stmt_id).as(relatedurl*)
    }
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
      0 < SQL("UPDATE relatedurl SET lastseen = {date) WHERE id = {id}").on('date -> date, 'id -> id).executeUpdate()
    }
  }
}
