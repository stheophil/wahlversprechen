package models

import models.Rating._
import java.util.Date
import eu.henkelmann.actuarius.{Transformer, Decorator}

import play.api.templates.Html
import play.api.Play
import play.api.Play.current

object Formatter {

	def glyph(rating: Rating) : String = {
		rating match {
			case PromiseKept => "glyph-icon-thumbs-up"
			case Compromise => "glyph-icon-adjust"
			case PromiseBroken => "glyph-icon-thumbs-down"
			case Stalled => "glyph-icon-time"
			case InTheWorks => "glyph-icon-cog"
			case Unrated => "glyphicon-question-sign"
		}
	}

	def color(rating: Rating) : String = {
		rating match {
			case PromiseKept => "#5cb85c"
			case Compromise => "#f0ad4e"
			case PromiseBroken => "#d9534f"
			case Stalled => "#d9984f"
			case InTheWorks => "#5bc0de"
			case Unrated => "#aaaaaa"
		}
	}

	def name(rating: Rating) : String = {
		rating match {
			case PromiseKept => "Gehalten"
			case Compromise => "Kompromiss"
			case PromiseBroken => "Gebrochen"
			case Stalled => "Blockiert"
			case InTheWorks => "In Arbeit"
			case Unrated => "Unbewertet"
		}
	}

	def url : String = "http://wahlversprechen.de"
	def disqus_shortname : String = Play.configuration.getString("disqus.shortname").get

	def format(date: Date)(implicit lang: play.api.i18n.Lang) : String = {
		new java.text.SimpleDateFormat("dd.MM.yy 'um' HH:mm", lang.toLocale).format(date)
	}

	private object FilterHeadlineFromMarkdown extends Decorator {		
	    override def allowVerbatimXml():Boolean = false		    
	    override def decorateImg(alt:String, src:String, title:Option[String]):String = ""		    
	    override def decorateRuler():String = ""		    
	    override def decorateHeaderOpen(headerNo:Int):String = "<div style='display: none'>"
	    override def decorateHeaderClose(headerNo:Int):String = "</div>"
	    override def decorateCodeBlockOpen():String = "<div 'display: none'>"
	    override def decorateCodeBlockClose():String = "<div 'display: none'>"
	}
	private object markdownToHTMLWithoutHeadlines extends Transformer {
		 override def deco() : Decorator = FilterHeadlineFromMarkdown
	}

	private object FilterXMLFromMarkdown extends Decorator {		
	    override def allowVerbatimXml():Boolean = false		    
	}
	private object markdownToHTML extends Transformer {
		 override def deco() : Decorator = FilterXMLFromMarkdown
	}

	def transformBodyToHTML(markdown: String) : Html = {
		Html(markdownToHTMLWithoutHeadlines(markdown))
	}

	def transformToHTML(markdown: String) : Html = {
		Html(markdownToHTML(markdown))
	}
}