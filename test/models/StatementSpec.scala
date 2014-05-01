package models

import org.specs2.mutable._

import play.api.Play.current
import play.api.db._
import java.util.Date
import org.specs2.specification.Scope
import helpers.DateFactory._
import helpers.WithTestDatabase

class StatementSpec extends Specification with WithTestDatabase {

  "create" should {
    "throw exception when creating unrated statement from rated author" in {
      val ratedAuthor = Author.create("rated author", 1, rated = true, "#000000", "#ffffff")
      val category = Category.create("some category", 1)

      Statement.create("title", ratedAuthor, category, None, None, None, None) must throwA[IllegalArgumentException]
    }

    "throw exception when creating merged statement from rated author" in {
      val ratedAuthor = Author.create("rated author", 1, rated = true, "#000000", "#ffffff")
      val category = Category.create("some category", 1)
      val statement = Statement.create("title", ratedAuthor, category, None, None, Some(Rating.InTheWorks), None)

      {
        Statement.create("title", ratedAuthor, category, None, None, Some(Rating.InTheWorks), Some(statement.id))
      } must throwA[IllegalArgumentException]
    }
  }

  trait TestStatements extends Scope {
    val userA = User.create("user.a@test.de", "user a", "secret", Role.Editor)

    val authorA = Author.create("Author A", 1, rated = false, "#ffffff", "#000000")
    val authorB = Author.create("Author B", 2, rated = false, "#ffffff", "#000000")

    val catA = Category.create("category A", 1)
    val catB = Category.create("category B", 2)

    var statementA = Statement.create("Statement A", authorA, catA, None, None, None, None)
    val statementB = Statement.create("Statement B", authorB, catB, None, None, None, None)
    val statementC = Statement.create("Statement C", authorB, catB, None, None, None, Some(statementB.id))

    val statementsByAuthor = Map(
      authorA -> List(statementA),
      authorB -> List(statementB, statementC)
    )

    val mergedStatements = Map(
      statementA -> Seq(),
      statementB -> Seq(statementB, statementC),
      statementC -> Seq()
    )

    val statementsByCategory = Map(
      catA -> Seq(statementA),
      catB -> Seq(statementB, statementC)
    )
  }

  "all" should {
    "return all stored statements by author" in new TestStatements {
      Statement.all must beEqualTo(statementsByAuthor)
    }

    "return empty map when no statements are in database" in {
      Statement.all must beEmpty
    }
  }

  "load" should {
    "return statements by id" in new TestStatements {
      Statement.load(statementA.id) must beSome(statementA)
    }

    "return None when given invalid id" in {
      Statement.load(4123) must beNone
    }
  }

  "loadAll" should {
    "return all merged statements" in new TestStatements {
      Statement.loadAll(statementB.id) must containAllOf(mergedStatements.get(statementB).get)
    }

    "return empty list when given invalid id" in {
      Statement.loadAll(4123) must beEmpty
    }
  }

  "withEntries" should {
    // TODO should be optimized
    "return a statement with loaded entries" in new TestStatements {
      val author = User.create("user@test.de", "testuser", "secret", Role.Editor)
      val entryAId = Entry.create(statementA.id, "content A", new Date(), author.id)
      val entryBId = Entry.create(statementA.id, "content B", new Date(), author.id)

      val loadedStatement = Statement.withEntries(statementA)

      val expectedEntries = Seq(Entry.load(entryAId).get, Entry.load(entryBId).get)

      loadedStatement.entries must containAllOf(expectedEntries)
    }
  }

  "find" should {
    "return statements matching case-insensitively given text" in new TestStatements {
      val statement = Statement.create("test statement", authorA, catB, Some("some text so test searching"), None, None, None)

      Statement.find("some text") must havePair(authorA -> List(statement))
    }

    "return empty map when nothing was found" in {
      Statement.find("some text") must beEmpty
    }

    "find statements by entry date" in {
      todo
    }

    "find statements by important tag" in {
      todo
    }

    "find statements by rating" in {
      todo
    }

    "count ratings by author" in {
      todo
    }
  }

  "byCategory" should {
    "find statements by category" in new TestStatements {
      Statement.byCategory(catA.name, None, None) must contain(statementA)
      Statement.byCategory(catB.name, None, None) must containAllOf(Seq(statementB, statementC))
    }

    "find statements by category and author" in new TestStatements {
      Statement.byCategory(catA.name, Some(authorA), None) must contain(statementA)
      Statement.byCategory(catA.name, Some(authorB), None) must beEmpty
    }

    "limit result size when required" in new TestStatements {
      Statement.byCategory(catB.name, None, Some(1)) must containAllOf(Seq(statementB))
    }
  }

  class TaggedTestStatements extends Before with TestStatements {
    lazy val tagA = Tag.create("tag A")
    lazy val tagB = Tag.create("tag B")

    override def before: Any = {
      Tag.add(statementA.id, tagA)
      Tag.add(statementA.id, tagB)
      Tag.add(statementB.id, tagA)
    }
  }

  "byTag" should {
    "find statements by tag" in new TaggedTestStatements {
      Statement.byTag(tagA.name, None, None).map(_.id) must containAllOf(Seq(statementA.id, statementB.id))
      Statement.byTag(tagB.name, None, None).map(_.id) must contain(statementA.id)
    }

    "find statements by tag and author" in new TaggedTestStatements {
      Statement.byTag(tagA.name, Some(authorA), None).map(_.id) must contain(statementA.id)
      Statement.byTag(tagB.name, Some(authorB), None).map(_.id) must beEmpty
    }

    "limit result size when required" in new TaggedTestStatements {
      Statement.byTag(tagA.name, None, Some(1)).map(_.id) must contain(statementA.id)
    }
  }

  "byImportantTag" should {
    "finds only statements with important tags" in new TaggedTestStatements {
      Tag.setImportant(tagB.id, important = true)

      Statement.byImportantTag(None, None).map(_.id) must containTheSameElementsAs(Seq(statementA.id))
    }
  }

  "byRating" should {
    "find statements by rating" in {
      val author = Author.create("Author A", 1, rated = true, "#ffffff", "#000000")

      val category = Category.create("category", 1)

      val statementA = Statement.create("Statement A", author, category, None, None, Some(Rating.InTheWorks), None)
      val statementB = Statement.create("Statement B", author, category, None, None, Some(Rating.InTheWorks), None)
      Statement.create("Statement C", author, category, None, None, Some(Rating.PromiseBroken), None)

      val loadedIds = Statement.byRating(Rating.InTheWorks, None, None).map(_.id)

      loadedIds must containTheSameElementsAs(Seq(statementA, statementB).map(_.id))
    }
  }

  "byEntryDate" should {
    "find statements by entry date" in new TestStatements {
      // latest entry for statementA is '2014 \ 10 \ 10'
      Entry.create(statementA.id, "some content", 2014 \ 10 \ 10, userA.id)
      Entry.create(statementA.id, "some content", 2014 \ 10 \ 7, userA.id)
      Entry.create(statementA.id, "some content", 2014 \ 10 \ 6, userA.id)

      // latest entry for statementB is '2014 \ 10 \ 11'
      Entry.create(statementB.id, "some content", 2014 \ 10 \ 11, userA.id)
      Entry.create(statementB.id, "some content", 2014 \ 10 \ 9, userA.id)

      Statement.byEntryDate(None, None).map(_.id) should beEqualTo(List(statementB.id, statementA.id))
    }
  }

  "setRating" should {
    "change a statements rating" in new TestStatements {
      DB.withConnection {
        implicit c =>
          Statement.setRating(c, statementA.id, Rating.Stalled, new Date())

          Statement.load(statementA.id) must beSome.which(_.rating === Some(Rating.Stalled))
      }
    }

    "return 'true' when given valid id" in new TestStatements {
      DB.withConnection {
        implicit c =>
          val result = Statement.setRating(c, statementA.id, Rating.Stalled, new Date())

          result must beEqualTo(true)
      }
    }

    "return 'false' when given invalid id" in {
      DB.withConnection {
        implicit c =>
          val result = Statement.setRating(c, 123, Rating.Stalled, new Date())

          result must beEqualTo(false)
      }
    }
  }

  "delete" should {
    "remove a statement from the database" in new TestStatements {
      Statement.delete(statementA.id)

      Statement.load(statementA.id) must beNone
    }
  }

  "edit" should {
    "change a statements properties" in new TestStatements {
      DB.withConnection {
        implicit c =>
          Statement.edit(c, statementA.id, "new title", catB, Some("test quote"), None, Some(Rating.PromiseKept), None)

          Statement.load(statementA.id) must beSome.which(
            stmt => stmt.title === "new title" and
              stmt.category === catB and
              stmt.quote === Some("test quote") and
              stmt.rating === Some(Rating.PromiseKept)
          )
      }
    }
  }
}