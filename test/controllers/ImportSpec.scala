package controllers

import org.specs2.mutable._
import play.api.http.Status
import helpers.WithTestDatabase
import models.{Statement, Author, Role, User}

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class ImportSpec extends Specification with WithTestDatabase {
  val author = "Coalition Treaty"
  val sheet_no_title = "1geJD1RzvJbDzdXV1GeUrgzWy5046LD1MYJ_A2Z95UAA"
  val sheet_no_category = "180b9kR1U7kH8pi0C1NJkvGaJAdTuBsDaOD3_xz6v6f8"
  val sheet1 = "1GPzhaarBjNer9lCnQ-8_lwnamPrUp0MW1rkis8UBVw8"
  val sheet2 = "12ooSGJrHN6l3mvwMsA10fEetYhIIjLA_ZL57pXxJSxc"
  val sheet3 = "1DJbUAIz33ogxR_bhgiXTFYdE4z1-QkYvLSTltz1Yd24"
  val sheet4 = "1us0DafZsza8zH3mG8cYZNTPnl-iM8g2WXMzU1-h4an4"

  trait TestData extends Before {
    override def before: Any = {
      User.create("test@test.net", "Tester", "secret", Role.Admin)

      Author.create("Coalition Treaty", 1, true, "#ffffff", "#999999")
    }
  }

  "controllers.Import.loadSpreadSheet" should {
    "return an error when a statement title is missing and not import anything" in new TestData {
      (controllers.Import.loadSpreadSheet(author, sheet_no_title).header.status must beEqualTo(Status.BAD_REQUEST)) and
        (models.Statement.all().values.flatten.size must beEqualTo(0))
    }
    "return an error when a category is missing" in new TestData {
      (controllers.Import.loadSpreadSheet(author, sheet_no_category).header.status must beEqualTo(Status.BAD_REQUEST)) and
        (models.Statement.all().values.flatten.size must beEqualTo(0))
    }
    "import new statements" in new TestData {
      val result = controllers.Import.loadSpreadSheet(author, sheet1)
      val stmts: Iterable[Statement] = models.Statement.all().values.flatten

      (result.header.status must beEqualTo(Status.OK)) and
        (stmts.size must beEqualTo(3)) and
        (stmts.last.title.length must beEqualTo(255)) and
        (stmts.last.quote.get.length must beEqualTo(8096)) and
        (stmts.last.quote_src.get.length must beEqualTo(1024)) and
        (stmts.last.category.name.length must beEqualTo(255))
    }
    "create new categories" in new TestData {
      (controllers.Import.loadSpreadSheet(author, sheet1).header.status must beEqualTo(Status.OK)) and
        (models.Category.loadAll().size must beEqualTo(2))
    }
    "create new tags" in new TestData {
      (controllers.Import.loadSpreadSheet(author, sheet1).header.status must beEqualTo(Status.OK)) and
        (models.Tag.loadAll().size must beEqualTo(4))
    }
    "update existing statements" in new TestData {
      val importOnce = controllers.Import.loadSpreadSheet(author, sheet1)
      val stmts: Iterable[Statement] = models.Statement.all().values.flatten
      val categories = models.Category.loadAll()
      val tags = models.Tag.loadAll()

      importOnce.header.status must beEqualTo(Status.OK)

      val importTwice = controllers.Import.loadSpreadSheet(author, sheet2)
      val stmtsNew: Iterable[Statement] = models.Statement.all().values.flatten
      val categoriesNew = models.Category.loadAll()
      val tagsNew = models.Tag.loadAll()

      importTwice.header.status must beEqualTo(Status.OK)

      val titleChanged = "Election Promise No 3XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
      val quoteNew = "I can't fix the title anymore but I can change the quote and reimport"
      val quotesrcNew = "The source seemed too long too"
      val categoryNew = "Internal Affairs"
      val tagNew = List("short", "list")

      val ostmtNew = stmtsNew.find(_.title == titleChanged)

      // TODO: Test SetMergedID incl ForeignKey Exception with sheet3/sheet4
      // TODO: When a statement is updated, the authors should match

      (stmtsNew.filter(_.title != titleChanged) must beEqualTo(stmts.filter(_.title != titleChanged))) and
        (categoriesNew.filter(_.name != categoryNew) must beEqualTo(categories.filter(_.name != categoryNew))) and
        (tagsNew.map(_.name).sorted must beEqualTo((tags.map(_.name) ++ tagNew).sorted)) and
        (ostmtNew must beSome) and
        (ostmtNew.get.quote.get must beEqualTo(quoteNew)) and
        (ostmtNew.get.quote_src.get must beEqualTo(quotesrcNew)) and
        (ostmtNew.get.rating must beNone) and
        (ostmtNew.get.rated must beNone) and
        (ostmtNew.get.latestEntry must beNone) and
        (ostmtNew.get.merged_id must beNone) and
        (ostmtNew.get.category.name must beEqualTo(categoryNew)) and
        (ostmtNew.get.tags.map(_.name).sorted.intersect(tagNew.sorted) must beEqualTo(tagNew.sorted))
    }
  }
}