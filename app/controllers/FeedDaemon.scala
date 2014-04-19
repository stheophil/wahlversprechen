package controllers;

import java.util.Date
import play.api.cache._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._

import net.theophil.relatedtexts._


object FeedDaemon {
  private val keyCachedMatcher = "FeedDaemon.cachedMatcher"
  private val keyStatements = "FeedDaemon.statements"
  private val keyMatches = "FeedDaemon.matches"

  private def removeMarkdownLink(text: String) : String = {
    val markdownLink = """\[([^\]]*)\]\(([^\)]*)\)""".r("text", "href")
    markdownLink.replaceAllIn( text, _ group "text" )
  }

  def matches = Action { implicit request =>
    Ok(Cache.get(keyMatches) match {
      case Some(o)  => o.asInstanceOf[String]
      case None => ""
    })
  }

	def update() {
		val feeds = List(
			"http://rss.sueddeutsche.de/rss/Politik",
			"http://www.welt.de/politik/deutschland/?service=Rss",
			"http://www.welt.de/wirtschaft/?service=Rss",
			"http://rss.sueddeutsche.de/rss/Wirtschaft",
			"http://newsfeed.zeit.de/politik/index",
			"http://newsfeed.zeit.de/wirtschaft/index",
			"http://newsfeed.zeit.de/gesellschaft/index",
			"http://www.faz.net/rss/aktuell/politik/",
			"http://www.faz.net/rss/aktuell/wirtschaft",
			"http://www.bundesregierung.de/SiteGlobals/Functions/RSSFeed/DE/RSSNewsfeed/RSS_Breg_artikel/RSSNewsfeed.xml?nn=392282"
    )

    var timeStart = new Date().getTime()

    case class InputStatement(id: Long, title: String, override val text: String, override val keywords: Seq[String]) extends Analyzable
		val cachedMatcher = Cache.get(keyCachedMatcher).map( _.asInstanceOf[FeedMatcherCache[InputStatement]] )
    val statements = Cache.getOrElse(keyStatements, 60 * 60 * 4) {
      models.Statement.all().flatMap(_._2).map {
        stmt =>
          InputStatement(
            stmt.id,
            stmt.title,
            removeMarkdownLink(stmt.title + " " + stmt.quote.getOrElse("")),
            stmt.tags.map(_.name)
          )
      }.toSeq
    }

    var timeEnd = new Date().getTime()
    Logger.info("FeedDaemon: Loading all "+ statements.size +" statements took " + (timeEnd - timeStart) + " ms")

    timeStart = new Date().getTime()
    FeedMatcher(feeds, statements, 100, cachedMatcher) match {
      case (results, cache) => {
        timeEnd = new Date().getTime()
        Logger.info("FeedDaemon: Analyzing all "+ feeds.size +" feeds took " + (timeEnd - timeStart) + " ms")

        timeStart = new Date().getTime()
        val jsonMatches = "{ lastGenerated: " + models.Formatter.formatRFC822(new Date()) +
          ", matches : " + Json.toJson(results).toString() +
          " }"

        Cache.set(keyCachedMatcher, cache)
        Cache.set(keyMatches, jsonMatches)
        timeEnd = new Date().getTime()
        Logger.info("FeedDaemon: Converting output to JSON and storing in cache took " + (timeEnd - timeStart) + " ms")
      }
	  }
	}
}
