package test

import models._
import org.specs2.execute._
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class JSONSpec extends Specification with WithFilledTestDatabase  {
  "The JSON API" should {
    "return all statements by an author" in {
    	val author = Author.loadRated.get
        val home = route(FakeRequest(GET, "/json/items/" + models.Formatter.MIMEEncode(author.name))).get
        
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "application/json")
        todo
    }
    "return all tags" in {
        val home = route(FakeRequest(GET, "/json/tags")).get
        
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "application/json")
        todo
    }
    "return all categories" in {
        val home = route(FakeRequest(GET, "/json/categories")).get
        
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "application/json")
        todo
    }
    "return a single statement" in {
    	val stmt = Statement.all().values.head.head
        val home = route(FakeRequest(GET, "/json/item/"+stmt.id)).get
        
        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "application/json")
        todo
    }
  }
}