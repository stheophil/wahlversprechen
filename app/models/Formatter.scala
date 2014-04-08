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
			case PromiseKept => "glyphicon-thumbs-up"
			case Compromise => "glyphicon-adjust"
			case PromiseBroken => "glyphicon-thumbs-down"
			case Stalled => "glyphicon-time"
			case InTheWorks => "glyphicon-cog"
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

	def icon(rating: Rating)(implicit request: play.api.mvc.RequestHeader) : String = {
		val file = rating match {
			case PromiseKept => "kept"
			case Compromise => "compromise"
			case PromiseBroken => "broken"
			case Stalled => "stalled"
			case InTheWorks => "intheworks"
			case Unrated => "unrated"
		}
		controllers.routes.Assets.at("img/ratings/" + file + ".png").absoluteURL(false)
	}

	def url : String = Play.configuration.getString("url").get
	def twitter : String = Play.configuration.getString("twitter").get
	def mail : String = Play.configuration.getString("mail").get
	def disqus_shortname : String = Play.configuration.getString("disqus.shortname").get

	def encode(url: String) = java.net.URLEncoder.encode(url, "UTF-8")

	def format(date: Date)(implicit lang: play.api.i18n.Lang) : String = {
		new java.text.SimpleDateFormat("dd.MM.yy", lang.toLocale).format(date)
	}

	def socialMetaTags(url: String, description: String, img: String) : Html = {
		Html(
		    "<meta property=\"og:url\" content=\"" + url + "\">\n" +
		    "<meta property=\"og:description\" content=\"" + description + "\">\n" +
		    "<meta property=\"twitter:description\" content=\"" + description + "\">\n" +
		    "<meta property=\"og:image\" content=\"" + img + "\">\n" + 
		    "<meta property=\"twitter:image\" content=\"" + img + "\">\n"
		)
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

	private object FilterXMLFromMarkdown extends Decorator {		
	    override def allowVerbatimXml():Boolean = false		    
	}

	private object FilterPlainTextFromMarkdown extends Decorator {
   		override def allowVerbatimXml():Boolean = false
		override def decorateBreak():String = ""
   	   	override def decorateCode(code:String):String = code
    	override def decorateEmphasis(text:String):String = text
    	override def decorateStrong(text:String):String = text
    	override def decorateLink(text:String, url:String, title:Option[String]):String = text
    	override def decorateImg(alt:String, src:String, title:Option[String]):String = ""
    	override def decorateRuler():String = ""
    	override def decorateHeaderOpen(headerNo:Int):String = ""
    	override def decorateHeaderClose(headerNo:Int):String = ""
    	override def decorateCodeBlockOpen():String = ""
    	override def decorateCodeBlockClose():String = ""
        override def decorateParagraphOpen():String = ""
		override def decorateParagraphClose():String = ""
		override def decorateBlockQuoteOpen():String = ""
		override def decorateBlockQuoteClose():String = ""
		override def decorateItemOpen():String = ""
		override def decorateItemClose():String = ""
		override def decorateUListOpen():String = ""
		override def decorateUListClose():String = ""
		override def decorateOListOpen():String = ""
		override def decorateOListClose():String = ""
	}

	private object markdownToHTMLWithoutHeadlines extends Transformer {
		 override def deco() : Decorator = FilterHeadlineFromMarkdown
	}
	def transformBodyToHTML(markdown: String) : Html = {
		Html(markdownToHTMLWithoutHeadlines(markdown))
	}

	private object markdownToHTML extends Transformer {
		 override def deco() : Decorator = FilterXMLFromMarkdown
	}
	def transformToHTML(markdown: String) : Html = {
		Html(markdownToHTML(markdown))
	}

	private object markdownToText extends Transformer {
		 override def deco() : Decorator = FilterPlainTextFromMarkdown
	}
	def transformToText(markdown: String) : String = {
		markdownToText(markdown)
	}
	
	
}
