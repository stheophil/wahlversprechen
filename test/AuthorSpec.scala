package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

import models._

 class AuthorSpec extends Specification with WithTestDatabase  {

  "Author" should {
    "return all records" in {
      Author.loadAll().size must beEqualTo(3)
    }
    "return record by id" in {
      val author = Author.loadAll().head
      Author.load(author.id) must beEqualTo(Some(author))
    }
    "return record by name" in {
      val author = Author.loadAll().head
      Author.load(author.name) must beEqualTo(Some(author))
    }
    "return rated author" in {
      val author = Author.loadAll().find(_.rated)      
      val authorLoaded = Author.loadRated()
      author must beSome
      author must beEqualTo(authorLoaded)
    }
    "create new author" in {
      val cAuthors = Author.loadAll().size

      val authorNew = Author(-1, "New Political Hope", 5, false, "#00000", "#999999")
      val authorCreated = Author.create(authorNew.name, authorNew.order, authorNew.rated, authorNew.color, authorNew.background)
      val authorLoaded = Author.load(authorCreated.id)
      
      Author.loadAll().size === cAuthors + 1
      Some(authorCreated) === authorLoaded
      authorNew.copy(id = authorCreated.id) === authorCreated
    }
    "edit author" in {
      val cAuthors = Author.loadAll().size
      val authorCreated = Author.create("New Political Hope", 5, false, "#00000", "#999999")
      val author = authorCreated.copy( name = "Another Party", order = 5, color = "#11111", background = "#222")

      val bEdited = Author.edit(author.id, author.name, author.order, author.rated, author.color, author.background)
      val authorEdited = Author.load(author.id)

      Author.loadAll().size === cAuthors + 1
      bEdited === true
      Some(author) === authorEdited
    }
    "return false when editing non-existing author" in {
      val maxId = Author.loadAll().map(_.id).max + 1
      val bEdited = Author.edit(maxId, "New Political Hope", 5, false, "#00000", "#999999")

      bEdited === false
    }
  }  
}