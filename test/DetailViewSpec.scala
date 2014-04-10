package test

import models._
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class DetailViewSpec extends Specification with WithFilledTestDatabase  {
  "DetailView" should {
    "render the detail page" in {
        val home = route(FakeRequest(GET, "/item/4")).get
        
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
        contentAsString(home) must contain ("twitter:description\" content=\"&quot;That is a quote with a markdown link&quot;\"")
        contentAsString(home) must contain ("og:description\" content=\"&quot;That is a quote with a markdown link&quot;\"")
        contentAsString(home) must contain ("That is a quote with a <a href=\"http://www.wikipedia.org\">markdown link</a>")
    }
    "update an item with a rating" in {
        val user = User.findAll().find(_.role == Role.Admin).get
        
        for(i <- -1 to Rating.maxId) {
            val stmtPrev = Statement.load(2).get
            val update = route(FakeRequest(PUT, "/item/2").
                withSession("email" -> user.email).
                withFormUrlEncodedBody("rating" -> i.toString)            
            ).get
            if(-1 == i || i == Rating.maxId) {
                status(update) must equalTo(BAD_REQUEST)
                Statement.load(2).get must equalTo(stmtPrev)
            } else {
                status(update) must equalTo(OK)
                Statement.load(2).get.rating.get.id must beEqualTo(i)
            }
        }
    }
  }
}