package test

import models._

import org.specs2.mutable._

import play.api.Play.current
import play.api.db._
import play.api.mvc.Results._
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

class StatementSpec extends Specification with WithFilledTestDatabase  {
  val author = "Coalition Treaty"

  "models.Statement" should {
    "return all statements by author" in {
      val stmts = Statement.all()
      (stmts.map(_._2.size) must beEqualTo(List(3, 3, 3)))
    }
    "return statements by id" in {
      val stmts = Statement.all()
      val stmtById = Statement.load(stmts.values.head.head.id)
      stmtById.get must beEqualTo(stmts.values.head.head)
    }
    "return all linked statements by id" in {
      val stmtsById = Statement.loadAll(7)
      stmtsById.size must beEqualTo(3)
    }
    "load entries for a statement" in {      
      val stmts = Statement.all()
      val stmt = stmts.values.head.head
      val content = "Super long [markdown](http://www.wikipedia.org) content"
      val date = new java.util.Date()
      val user = User.findAll().head

      val id = Entry.create(stmt.id, content, date, user.id)
      val entry = Entry(id, stmt.id, content, date, user)

      (stmt.copy(entries = List(entry)) must beEqualTo(Statement.withEntries(stmt)))
    }
    "find statements case-insensitively by text" in {      
      val stmts = Statement.all()
      val stmtsByAuthor1 = Statement.find("MARKdown link")
      val stmtsByAuthor2 = Statement.find("markdown")
      (stmtsByAuthor1 must beEqualTo(stmtsByAuthor2)) and
      (stmtsByAuthor1.values.flatten.size must beEqualTo(4))
    }
    "find statements by entry date" in {
      val stmts = Statement.all()
      Statement.byEntryDate(None, None)
      Statement.byEntryDate(Author.load(author), Some(3))
      todo
    }
    "find statements by important tag" in {      
      Statement.byImportantTag(None, None)
      Statement.byImportantTag(Author.load(author), Some(3))
      val stmts = Statement.all()
      todo
    }    
    "find statements by tag" in {      
      val stmtsByTag = Statement.byTag("comma-separated-list", None, None)
      val stmtsByTag2 = Statement.byTag("comma-separated-list", Author.load(author), Some(1))

      (stmtsByTag.size === 6) &&
      (stmtsByTag.forall( _.tags.map(_.name).contains("comma-separated-list"))) and 
      (stmtsByTag2.size === 1) &&
      (stmtsByTag2.forall( _.tags.map(_.name).contains("comma-separated-list")))
    }    
    "find statements by category" in {    
      val stmts1 = Statement.byCategory("Föreign Äffärs", None, None)
      val stmts2 = Statement.byCategory("Föreign Äffärs", Author.load(author), Some(1))
      (stmts1.size must beEqualTo(6)) &&
      (stmts1.forall( _.category.name.equals("Föreign Äffärs"))) && 
      (stmts2.size must beEqualTo(1))
    }
    "find statements by rating" in {      
      Statement.byRating(Rating.Unrated, None, None)
      Statement.byRating(Rating.Unrated, Author.load(author), Some(3))
      val stmts = Statement.all()
      todo
    }
    "count ratings by author" in {
      val stmts = Statement.all()
      Statement.countRatings(Author.load(author).get)
      todo
    }
    "update rating" in {
      val stmt = Statement.all().values.head.head
      DB.withConnection { implicit c => 
        Statement.setRating(c, stmt.id, Rating.PromiseKept, new java.util.Date()) must beEqualTo(true)
        Statement.load(stmt.id).get.rating must beEqualTo(Some(Rating.PromiseKept))
      }
    }
    "not update rating for invalid id" in {
      val stmt = Statement.all().values.head.head
      DB.withConnection { implicit c => 
        Statement.setRating(c, -1, Rating.PromiseKept, new java.util.Date()) must beEqualTo(false)
        Statement.load(stmt.id).get must beEqualTo(stmt)
      }
    }
    // TODO: Test editing
  }  
}