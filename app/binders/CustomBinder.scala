package binders

import java.util.Date
import play.api.mvc.QueryStringBindable
import models.Formatter

object DateBinder {
	implicit val queryStringBinder = new QueryStringBindable[Date] {
	  override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Date]] = {
	  	params.get(key).map{ seq =>
	  		try {
		  		Right( Formatter.DayDateFormatter.parse(seq.head) )
		  	} catch {
		  		case e: Exception => Left("Unable to bind date")
		  	}	
	  	}
	  }
	  override def unbind(key: String, date: Date): String = {
	    Formatter.DayDateFormatter.format(date)
	  }
	}
}