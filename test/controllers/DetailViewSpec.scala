package controllers

import models._
import org.specs2.execute._
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import helpers.WithFilledTestDatabase

class DetailViewSpec extends Specification with WithFilledTestDatabase {
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


  "DetailView" should {
    "render the detail page" in {
      val author = Author.create("test author", 1, false, "#ffffff", "#000000")
      val categoty = Category.create("test category", 1)
      val stmt = Statement.create("test statement", author, categoty,
        Some("That is a quote with a [markdown link](http://www.wikipedia.org)"), None, None, None)
      val home = route(FakeRequest(GET, "/item/" + stmt.id)).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("twitter:description\" content=\"That is a quote with a markdown link")
      contentAsString(home) must contain("og:description\" content=\"That is a quote with a markdown link")
      contentAsString(home) must contain("That is a quote with a <a href=\"http://www.wikipedia.org\">markdown link</a>")
    }

    "return BAD_REQUEST on invalid update input and not change the statement" in {
      val ratedAuthor = Author.create("rated author", 1, true, "#ffffff", "#000000")
      val unratedAuthor = Author.create("unrated author", 1, false, "#ffffff", "#000000")

      val categoty = Category.create("test category", 1)
      val unratedStmt = Statement.create("unrated test statement", ratedAuthor, categoty,
        Some("some text"), None, None, None)

      val ratedStmt = Statement.create("rated test statement", ratedAuthor, categoty,
        Some("some text"), None, Some(Rating.Compromise), None)

      //      val user = User.findAll().find(_.role == Role.Admin).get
      val user = User.create("test.user@test.de", "test", "secret", Role.Admin)
      val stmtsUnrated = Statement.all().find(!_._1.rated).get._2
      val stmtsRated = Statement.all().find(_._1.rated).get._2

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
              "merged_id" -> stmtsUnrated.head.id
            ),
          stmtsRated.last ->
            List(
              "merged_id" -> stmtsUnrated.head.id,
              "merged_id" -> stmtsRated.head.id
            )
        ),
        "email" -> user.email
      )
    }

    "return OK on valid update input and change the statement" in {
      val user = User.findAll().find(_.role == Role.Admin).get
      val stmtsUnrated = Statement.all().find(!_._1.rated).get._2
      val stmtsRated = Statement.all().find(_._1.rated).get._2

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
              ("merged_id", List(stmtsRated.head.id), _.merged_id.get)
            )
        ),
        "email" -> user.email
      )
    }
  }
}