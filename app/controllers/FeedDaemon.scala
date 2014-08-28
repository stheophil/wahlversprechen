package controllers;

import java.util.Date
import play.api.Logger
import play.api.Play
import play.api.Play.current
import scala.collection.JavaConverters._

import models._
import net.theophil.relatedtexts._

@SerialVersionUID(1l)
case class InputStatement(id: Long, title: String, override val text: String, override val keywords: Seq[String]) extends Analyzable

object FeedDaemon {
  private val fileCachedMatcher = "tmp/wahlversprechen_cachedMatcher"
  private val keyStatements = "FeedDaemon.statements"

  def removeMarkdownLink(text: String) : String = {
    val markdownLink = """\[([^\]]*)\]\(([^\)]*)\)""".r("text", "href")
    markdownLink.replaceAllIn( text, _ group "text" )
  }

	def update() {
		val feeds = Play.configuration.getStringList("application.feeds").map( _.asScala.toList ).getOrElse(List.empty[String])
    val fMinScore = Play.configuration.getDouble("application.feed_min_score").getOrElse(1.0)

    if(feeds.isEmpty) return

    var timeStart = new Date().getTime()

		val cachedMatcher = net.theophil.relatedtexts.Cache.fromFile[FeedMatcherCache[InputStatement]](fileCachedMatcher)

    val statements = play.api.cache.Cache.getOrElse(keyStatements, 60 * 60 * 24) {
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

    def foreachMatch(m: (InputStatement, TextMatch[FeedMatcherCache.AnalyzableItem])): Unit = {
        RelatedUrl.loadByUrl(m._1.id, m._2.matched.link) match {
          case Some(relatedurl) =>
            RelatedUrl.update(relatedurl.id, new Date())
          case None =>
            RelatedUrl.create(m._1.id, m._2.matched.title, m._2.matched.link, m._2.value, new Date(), RelatedCategory.Article)
        }
    }

    val cache = FeedMatcher(statements, feeds, foreachMatch, fMinScore, cachedMatcher)
    net.theophil.relatedtexts.Cache.toFile(fileCachedMatcher, cache)

    timeEnd = new Date().getTime()
    Logger.info("FeedDaemon: Parsing feeds took " + (timeEnd - timeStart) + " ms")
	}
}
