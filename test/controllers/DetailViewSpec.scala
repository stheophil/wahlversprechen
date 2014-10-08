package controllers

import models._
import org.specs2.execute._
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import helpers.WithTestDatabase
import org.specs2.specification.Scope

class DetailViewSpec extends Specification with WithTestDatabase {
  def verifyInvalidUpdate[T](result: Int, url: (T) => String, load: (T) => T, invalid_args: Map[T, List[(String, Any)]], session: (String, String)) =
    Result.unit(for ((t, args) <- invalid_args) {
      for ((param, value) <- args) {
        val update = route(FakeRequest(PUT, url(t)).
          withSession(session).
          withFormUrlEncodedBody(param -> value.toString)
        ).get

        status(update) aka ("status after setting " + param + " to " + value + " ") must equalTo(result)
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
    val userEditor = User.create("test2@test.de", "test", "password", Role.Editor)
    val userEditor2 = User.create("test3@test.de", "test", "password", Role.Editor)

    val author = Author.create("test author", 1, false, "#ffffff", "#000000")
    val category = Category.create("test category", 1)

    val stmt = Statement.create("test statement", author, category,
      Some("some text"), None)
    val stmtWithTag = Statement.create("test statement 2", author, category,
      Some("some text"), None)
    val tag = Tag.create("Only Tag");
    Tag.add(stmtWithTag.id, tag)

    val entryEditor = Entry.load( Entry.create(stmt.id, "content", new java.util.Date(), userEditor.id) ).get
    val entryEditor2 = Entry.load( Entry.create(stmt.id, "content", new java.util.Date(), userEditor2.id) ).get

    val topAuthor = Author.create("top level test author", 1, true, "#ffffff", "#000000")
    val topLevelStatement = Statement.create("test statement", topAuthor, category,
      Some("some text"), None)
  }

  "update" should {
    "return BAD_REQUEST on invalid update input and not change the statement" in new TestData {
      val stmtsUnrated = List(stmt)
      val stmtsRated = List(topLevelStatement)

      verifyInvalidUpdate[Statement](
        BAD_REQUEST,
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
    "return FORBIDDEN on when Editor tries to edit Statement data" in new TestData {
      val stmtsUnrated = List(stmt)

      verifyInvalidUpdate[Statement](
        FORBIDDEN,
        s => {
          "/item/" + s.id
        },
        s => {
          Statement.load(s.id).get
        },
        Map(
          stmtsUnrated.last ->
            List(
              "title" -> "Some title",
              "quote" -> "Some quote",
              "quote_src" -> "Some quote src",
              "linked_id" -> 0
            )
        ),
        "email" -> userEditor.email
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
  // TODO: Remove copy-paste code in following tests
  "delete" should {
    "delete statement when called by Admin" in new TestData {
        val update = route(FakeRequest(DELETE, "/item/"+stmt.id).
          withSession("email" -> user.email)
        ).get

        status(update) must equalTo(OK)
        Statement.load(stmt.id) must beNone
    }
    "return FORBIDDEN when called by Editor" in new TestData {
        val update = route(FakeRequest(DELETE, "/item/"+stmt.id).
          withSession("email" -> userEditor.email)
        ).get

        status(update) must equalTo(FORBIDDEN)
    }
    "return FORBIDDEN when called without user" in new TestData {
        val update = route(FakeRequest(DELETE, "/item/"+stmt.id)).get
        status(update) must equalTo(SEE_OTHER)
    }
  }
  "addEntry" should {
    "create new Entry when called by Editor" in new TestData {
        val text = "some content for the new entry"
        val update = route(FakeRequest(POST, "/item/"+stmt.id).
            withSession("email" -> userEditor.email).
            withFormUrlEncodedBody("content" -> text)
          ).get

        status(update) must equalTo(OK)

        val entries = Entry.loadByStatement(stmt.id)
        entries.length must equalTo(3)
        entries.head.content must equalTo(text)
        entries.head.user must equalTo(userEditor)
    }
    "return SEE_OTHER when called without user" in new TestData {
      val text = "some content for the new entry"
      val update = route(FakeRequest(POST, "/item/"+stmt.id).
        withFormUrlEncodedBody("content" -> text)
      ).get

      status(update) must equalTo(SEE_OTHER)
      Entry.loadByStatement(stmt.id) must equalTo(List(entryEditor2, entryEditor))
    }
  }
  "updateEntry" should {
    "edit Entry when called by Admin" in new TestData {
      val text = "some content for the new entry"
      val update = route(FakeRequest(PUT, "/entry/"+entryEditor.id).
          withSession("email" -> user.email).
          withFormUrlEncodedBody("content" -> text)
        ).get

      status(update) must equalTo(OK)
      Entry.load(entryEditor.id).get.content must equalTo(text)
    }
    "edit Entry when called by owning Editor" in new TestData {
      val text = "some content for the new entry"
      val update = route(FakeRequest(PUT, "/entry/"+entryEditor.id).
          withSession("email" -> userEditor.email).
          withFormUrlEncodedBody("content" -> text)
        ).get

      status(update) must equalTo(OK)
      Entry.load(entryEditor.id).get.content must equalTo(text)
    }
    "return FORBIDDEN when called other Editor" in new TestData {
      val text = "some content for the new entry"
      val update = route(FakeRequest(PUT, "/entry/"+entryEditor.id).
          withSession("email" -> userEditor2.email).
          withFormUrlEncodedBody("content" -> text)
        ).get

      status(update) must equalTo(FORBIDDEN)
      Entry.load(entryEditor.id).get must equalTo(entryEditor)
    }
    "return FORBIDDEN when called without user" in new TestData {
      val text = "some content for the new entry"
      val update = route(FakeRequest(PUT, "/entry/"+entryEditor.id).
          withFormUrlEncodedBody("content" -> text)
        ).get

      status(update) must equalTo(SEE_OTHER)
      Entry.load(entryEditor.id).get must equalTo(entryEditor)
    }
  }
  "deleteEntry" should {
    "delete Entry when called by Admin" in new TestData {
      val update = route(FakeRequest(DELETE, "/entry/"+entryEditor.id).
          withSession("email" -> user.email)
        ).get

      status(update) must equalTo(OK)
      Entry.load(entryEditor.id) must beNone
    }
    "return FORBIDDEN when called by Editor" in new TestData {
      val update = route(FakeRequest(DELETE, "/entry/"+entryEditor.id).
          withSession("email" -> userEditor.email)
        ).get

      status(update) must equalTo(FORBIDDEN)
      Entry.load(entryEditor.id).get must equalTo(entryEditor)
    }
    "return SEE_OTHER when called without user" in new TestData {
      val update = route(FakeRequest(DELETE, "/entry/"+entryEditor.id)).get
      status(update) must equalTo(SEE_OTHER)
      Entry.load(entryEditor.id).get must equalTo(entryEditor)
    }
  } 
  "addTag" should {
    "add tag when called by Admin" in new TestData {
        val tagNew ="New Tag"
        val update = route(FakeRequest(POST, "/item/"+stmt.id+"/tag").
          withSession("email" -> user.email).
          withFormUrlEncodedBody("name" -> tagNew)
        ).get

      status(update) must equalTo(OK)
      Statement.load(stmt.id).get.tags.map(_.name) must containTheSameElementsAs(List(tagNew))
    }
    "add tag when called by Editor" in new TestData {
      val tagNew ="New Tag"
        val update = route(FakeRequest(POST, "/item/"+stmt.id+"/tag").
          withSession("email" -> user.email).
          withFormUrlEncodedBody("name" -> tagNew)
        ).get

      status(update) must equalTo(OK)
      Statement.load(stmt.id).get.tags.map(_.name) must containTheSameElementsAs(List(tagNew))
    }
    "return SEE_OTHER when called without user" in new TestData {
      val tagNew ="New Tag"
        val update = route(FakeRequest(POST, "/item/"+stmt.id+"/tag").
          withFormUrlEncodedBody("name" -> tagNew)
        ).get

      status(update) must equalTo(SEE_OTHER)
      Statement.load(stmt.id).get.tags.size must equalTo(0)
    }
  }  
  "deleteTag" should {
    "delete tag when called by Admin" in new TestData {
      val update = route(FakeRequest(DELETE, "/item/"+stmtWithTag.id+"/tag/" + tag.id).
          withSession("email" -> user.email)
        ).get

      status(update) must equalTo(OK)
      Statement.load(stmtWithTag.id).get.tags.size must equalTo(0)
    }
    "delete tag when called by Editor" in new TestData {
      val update = route(FakeRequest(DELETE, "/item/"+stmtWithTag.id+"/tag/" + tag.id).
          withSession("email" -> user.email)
        ).get

      status(update) must equalTo(OK)
      Statement.load(stmtWithTag.id).get.tags.size must equalTo(0)
    }
    "return SEE_OTHER when called without user" in new TestData {
      val update = route(FakeRequest(DELETE, "/item/"+stmtWithTag.id+"/tag/" + tag.id)).get

      status(update) must equalTo(SEE_OTHER)
      Statement.load(stmtWithTag.id).get.tags.map(_.name) must containTheSameElementsAs(List(tag.name))
    }
  }
}