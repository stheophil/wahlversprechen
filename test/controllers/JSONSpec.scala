package controllers

import models._
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import helpers.{WithTestDatabase, WithFilledTestDatabase}

class JSONSpec extends Specification with WithTestDatabase {
  "The JSON API" should {
    "return all statements by an author" in {
      val author = Author.create("test author", 1, false, "#ffffff", "#000000")
      val home = route(FakeRequest(GET, "/json/items/" + models.Formatter.MIMEEncode(author.name))).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "application/json")
    }

    "return all tags" in {
      val home = route(FakeRequest(GET, "/json/tags")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "application/json")
    }

    "return all categories" in {
      val home = route(FakeRequest(GET, "/json/categories")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "application/json")
    }

    "return a single statement" in {
      val author = Author.create("test author", 1, false, "#ffffff", "#000000")
      val categoty = Category.create("test category", 1)
      val stmt = Statement.create("test statement", author, categoty, None, None, None, None)
      val home = route(FakeRequest(GET, "/json/item/" + stmt.id)).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "application/json")
    }
  }
}