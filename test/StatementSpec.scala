package test

import org.specs2.mutable._

import play.api.mvc.Results._
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
 class StatementSpec extends Specification with WithTestDatabase  {
  val author = "Koalitionsvertrag"

  // TODO: import sheet[2-4]
  // TODO: create some entries

  "models.Statement" should {
    "return all statements by author" in {
      val stmts = models.Statement.all()
      (stmts.map(_._2.size) must beEqualTo((3, 3, 3)))
    }
    "return statements by id" in {
      val stmts = models.Statement.all()
      // val stmtById = models.Statement.load(stmts.values.head.head.id)
      // stmtById must beEqualTo(stmts.values.head.head)
      false
    }
    "return all linked statements by id" in {      
      val stmts = models.Statement.all()
      // val stmtsById = models.Statement.loadAll(stmts.values.head.head.id)
      // stmtsById must beEqualTo(stmts.values.head.head)
      false
    }
    "load entries for a statement" in {      
      val stmts = models.Statement.all()
      // val stmtWithEntries = models.Statement.withEntries(stmts.values.head.head)
      // TODO
      false
    }
    "find statements case-insensitively by text" in {      
      val stmts = models.Statement.all()
      val stmtsByAuthor1 = models.Statement.find("MARKdown link")
      val stmtsByAuthor2 = models.Statement.find("markdown")
      (stmtsByAuthor1 must beEqualTo(stmtsByAuthor2)) and
      (stmtsByAuthor1.values.flatten.size must beEqualTo(4))
    }
    "find statements by entry date" in {
      val stmts = models.Statement.all()
      models.Statement.byEntryDate(None, None)
      models.Statement.byEntryDate(models.Author.load("Koalitionsvertrag"), Some(3))
      false
    }
    "find statements by important tag" in {      
      models.Statement.byImportantTag(None, None)
      models.Statement.byImportantTag(models.Author.load("Koalitionsvertrag"), Some(3))
      val stmts = models.Statement.all()
      false
    }    
    "find statements by tag" in {      
      val stmtsByTag = models.Statement.byTag("comma-separated-list", None, None)
      models.Statement.byTag("comma-separated-list", models.Author.load("Koalitionsvertrag"), Some(3))

      (stmtsByTag.size === 6) &&
      (stmtsByTag.forall( _.tags.map(_.name).contains("comma-separated-list")))
    }    
    "find statements by category" in {    
      models.Statement.byCategory("Föreign Äffärs", None, None)
      models.Statement.byCategory("Föreign Äffärs", models.Author.load("Koalitionsvertrag"), Some(3))
      val stmts = models.Statement.all()
      false
    }
    "find statements by rating" in {      
      models.Statement.byRating(models.Rating.Unrated, None, None)
      models.Statement.byRating(models.Rating.Unrated, models.Author.load("Koalitionsvertrag"), Some(3))
      val stmts = models.Statement.all()
      false
    }
    "count ratings by author" in {
      val stmts = models.Statement.all()
      models.Statement.countRatings(models.Author.load("Koalitionsvertrag").get)
      false
    }
  }  
}