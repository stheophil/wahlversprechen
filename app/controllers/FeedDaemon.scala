package controllers;

import java.util.Date
import play.api.cache._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logger
import play.api.Play
import play.api.Play.current
import scala.collection.JavaConverters._

import models._
import net.theophil.relatedtexts._

object FeedDaemon {
  private val fileCachedMatcher = "/tmp/wahlversprechen_cachedMatcher"
  private val keyStatements = "FeedDaemon.statements"

  private def removeMarkdownLink(text: String) : String = {
    val markdownLink = """\[([^\]]*)\]\(([^\)]*)\)""".r("text", "href")
    markdownLink.replaceAllIn( text, _ group "text" )
  }

  def matches = Action { implicit request =>
    Ok("")
  }

	def update() {
		val feeds = Play.configuration.getStringList("application.feeds").map( _.asScala.toList ).getOrElse(List.empty[String])
    val cBestMatches = Play.configuration.getInt("application.feed_match_count").getOrElse(25)

    if(cBestMatches <= 0 || feeds.isEmpty) return

    var timeStart = new Date().getTime()

    case class InputStatement(id: Long, title: String, override val text: String, override val keywords: Seq[String]) extends Analyzable

		val cachedMatcher = FeedMatcherCache.fromFile[InputStatement](fileCachedMatcher)

    val statements = Cache.getOrElse(keyStatements, 60 * 60 * 24) {
      models.Statement.all().flatMap(_._2).map {
        stmt =>
          InputStatement(
            stmt.id,
            stmt.title,
            removeMarkdownLink(stmt.title + " " + stmt.quote.getOrElse("")),
            stmt.tags.map(_.name).toSeq
          )
      }.toSeq
    }

    var timeEnd = new Date().getTime()
    Logger.info("FeedDaemon: Loading all "+ statements.size +" statements took " + (timeEnd - timeStart) + " ms")

    timeStart = new Date().getTime()
    FeedMatcher(feeds, statements, cBestMatches, cachedMatcher) match {
      case (results, cache) => {
        timeEnd = new Date().getTime()
        Logger.info("FeedDaemon: Analyzing all "+ feeds.size +" feeds took " + (timeEnd - timeStart) + " ms")

        timeStart = new Date().getTime()
        results.foreach{ r =>
          r.articles.filter( 
            _.confidence > Play.configuration.getDouble("application.feed_min_score").getOrElse(4.0)
          ).foreach{ article =>
            RelatedUrl.loadByUrl(r.text.id, article.url) match {
              case Some(relatedurl) =>
                RelatedUrl.update(relatedurl.id, new Date())
              case None =>
                RelatedUrl.create(r.text.id, article.title, article.url, article.confidence, new Date(), RelatedCategory.Article)
            }
          }
        }

        cache.serialize(fileCachedMatcher)
        timeEnd = new Date().getTime()
        Logger.info("FeedDaemon: Converting output to JSON and storing in cache took " + (timeEnd - timeStart) + " ms")
      }
	  }
	}
}
