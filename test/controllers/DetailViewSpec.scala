package controllers

import models._
import org.specs2.execute._
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import helpers.WithTestDatabase
import org.specs2.specification.Scope

class DetailViewSpec extends Specification with WithTestDatabase {
  def verifyInvalidUpdate[T](url: (T) => String, load: (T) => T, invalid_args: Map[T, List[(String, Any)]], session: (String, String)) =
    Result.unit(for ((t, args) <- invalid_args) {
      for ((param, value) <- args) {
        val update = route(FakeRequest(PUT, url(t)).
          withSession(session).
          withFormUrlEncodedBody(param -> value.toString)
        ).get

        status(update) aka ("status after setting " + param + " to " + value + " ") must equalTo(BAD_REQUEST)
        load(t) aka ("load(t) after setting " + param + " to " + value + " ") must equalTo(t)
      }
    })

  def verifyValidUpdate[T](url: (T) => String, load: (T) => T, valid_args: Map[T, List[(String, List[Any], (T) => Any)]], session: (String, String)) =
    Result.unit(for ((t, args) <- valid_args) {
      for ((param, values, fun) <- args) {
        for (value <- values) {
          val tBeforeUpdate = load(t)

          val update = route(FakeRequest(PUT, url(t)).
            withSession(session).
            withFormUrlEncodedBody(param -> value.toString)
          ).get

          status(update) aka ("status after setting " + param + " to " + value + " ") must equalTo(OK)

          val tAfterUpdate = load(t)
          fun(tAfterUpdate) aka ("fun(tAfterUpdate) after setting " + param + " to " + value + " ") must equalTo(value)
          for ((paramOther, valuesOther, funOther) <- args if paramOther != param) {
            funOther(tBeforeUpdate) aka (paramOther + " after setting " + param + " to " + value + " ") must equalTo(funOther(tAfterUpdate))
          }
        }
      }
    })

  "get" should {
    "return NOT_FOUND when statement not found" in {
      val Some(site) = route(FakeRequest(GET, "/item/12345"))

      status(site) must be equalTo NOT_FOUND
    }

    "return OK when statement was found" in {
      val author = Author.create("test author", 1, false, "#ffffff", "#000000")
      val categoty = Category.create("test category", 1)
      val stmt = Statement.create("test statement", author, categoty,
        Some("some text"), None)

      val Some(site) = route(FakeRequest(GET, s"/item/${stmt.id}"))

      status(site) must be equalTo OK
    }

    "render the detail page" in {
      val author = Author.create("test author", 1, false, "#ffffff", "#000000")
      val categoty = Category.create("test category", 1)
      val stmt = Statement.create("test statement", author, categoty,
        Some("That is a quote with a [markdown link](http://www.wikipedia.org)"), None)
      val home = route(FakeRequest(GET, s"/item/${stmt.id}")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("og:description\" content=\"That is a quote with a markdown link")
      contentAsString(home) must contain("That is a quote with a <a href=\"http://www.wikipedia.org\">markdown link</a>")
    }
  }

  trait TestData extends Scope {
    val user = User.create("test@test.de", "test", "password", Role.Admin)
    val author = Author.create("test author", 1, false, "#ffffff", "#000000")
    val categoty = Category.create("test category", 1)
    val stmt = Statement.create("test statement", author, categoty,
      Some("some text"), None)

    val topAuthor = Author.create("top level test author", 1, true, "#ffffff", "#000000")
    val topLevelStatement = Statement.create("test statement", topAuthor, categoty,
      Some("some text"), None)
  }

  "update" should {
    "return BAD_REQUEST on invalid update input and not change the statement" in new TestData {
      val stmtsUnrated = List(stmt)
      val stmtsRated = List(topLevelStatement)

      verifyInvalidUpdate[Statement](
        s => {
          "/item/" + s.id
        },
        s => {
          Statement.load(s.id).get
        },
        Map(
          stmtsUnrated.last ->
            List(
              "rating" -> -1,
              "rating" -> Rating.maxId,
              // "title" -> "",
              "linked_id" -> stmtsUnrated.head.id
            ),
          stmtsRated.last ->
            List(
              "linked_id" -> stmtsUnrated.head.id,
              "linked_id" -> stmtsRated.head.id
            )
        ),
        "email" -> user.email
      )
    }
    "return OK on valid update input and change the statement" in new TestData {
      val stmtsUnrated = List(stmt)
      val stmtsRated = List(topLevelStatement)

      verifyValidUpdate[Statement](
        s => {
          "/item/" + s.id
        },
        s => {
          Statement.load(s.id).get
        },
        Map(
          stmtsUnrated.head ->
            List(
              ("rating", List(0, Rating.maxId - 1), _.rating.get.id),
              ("title", List("Some title"), _.title),
              ("quote", List("And I can also set the Quote."), _.quote.get),
              ("quote_src", List("And I can also set the Quote Source."), _.quote_src.get),
              ("linked_id", List(stmtsRated.head.id), _.linked_id.get)
            )
        ),
        "email" -> user.email
      )
    }
  }
}