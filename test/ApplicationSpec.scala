package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends Specification with WithFilledTestDatabase  {
  "Application" should {
    
    "send 404 on a bad request" in {
        route(FakeRequest(GET, "/boum")) must beNone        
    }

    "render the index page" in {
        val home = route(FakeRequest(GET, "/")).get
        
        (status(home) must equalTo(OK)) and
        (contentType(home) must beSome.which(_ == "text/html"))
        (contentAsString(home) must contain ("wahlversprechen"))
    }
    "render the detail page" in {
        val home = route(FakeRequest(GET, "/item/4")).get
        
        (status(home) must equalTo(OK)) and
        (contentType(home) must beSome.which(_ == "text/html")) and
        (contentAsString(home) must contain ("twitter:description\" content=\"&quot;That is a quote with a markdown link&quot;\"")) and
        (contentAsString(home) must contain ("og:description\" content=\"&quot;That is a quote with a markdown link&quot;\"")) and        
        (contentAsString(home) must contain ("That is a quote with a <a href=\"http://www.wikipedia.org\">markdown link</a>"))
    }
  }
}