package models

import org.specs2.mutable._

import play.api.Play.current
import play.api.db._
import java.util.Date
import org.specs2.specification.Scope
import helpers.DateFactory._
import helpers.WithTestDatabase

class StatementSpec extends Specification with WithTestDatabase {

  trait TestStatements extends Scope {
    val userA = User.create("user.a@test.de", "user a", "secret", Role.Editor)

    val authorA = Author.create("Author A", 1, top_level = true, "#ffffff", "#000000")
    val authorB = Author.create("Author B", 2, top_level = false, "#ffffff", "#000000")

    val catA = Category.create("category A", 1)
    val catB = Category.create("category B", 2)

    var statementA = { 
        // authorA is top-level, hence stmt has Rating.Unrated by default
        // The date returned from Statement.create is not the same as the one
        // roundtripped through DB. Hence, load statementA from DB.
        val stmt = Statement.create("Statement A", authorA, catA, None, None)
        Statement.load(stmt.id).get
    }
    val statementB = Statement.create("Statement B", authorB, catB, None, None)
    val statementC = {
      val stmt = Statement.create("Statement C", authorB, catB, None, None)
      Statement.setLinkedID(stmt.id, statementA.id)
      Statement.load(stmt.id).get
    }

    val allStatements = Set(statementA, statementB, statementC)

    private def statementsFrom(author: Author): List[Statement] =
      allStatements.filter(_.author == author).toList

    val statementsByAuthor: Map[Author, List[Statement]] = {
      val authors: Set[Author] = allStatements.map(_.author)

      authors.map(author => (author, statementsFrom(author))).toMap
    }

    val mergedStatements = Map(
      statementA -> Seq(statementA, statementC),
      statementB -> Seq(),
      statementC -> Seq()
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

    // see issue #9: Tags shown several times when Statement has multiple entries
    "not have multiple tags" in new TestStatements {
      val tag1 = Tag.create("some tag")
      Tag.add(statementA.id, tag1)

      val entry1 = Entry.create(statementA.id, "some content", 2014 \ 4 \ 1, userA.id)
      val entry2 = Entry.create(statementA.id, "some content", 2014 \ 4 \ 1, userA.id)
      val entry3 = Entry.create(statementA.id, "some content", 2014 \ 4 \ 1, userA.id)

      val Some(loadedStatement) = Statement.load(statementA.id)

      loadedStatement.tags must haveSize(1)
    }
  }

  "loadAll" should {
    "return all merged statements" in new TestStatements {
      Statement.loadAll(statementA.id) must containAllOf(mergedStatements.get(statementA).get) 
    }

    "return empty list when given invalid id" in {
      Statement.loadAll(4123) must beEmpty
    }
  }

  "withEntriesAndRatings" should {
    // TODO should be optimized
    "return a statement with loaded entries" in new TestStatements {
      val author = User.create("user@test.de", "testuser", "secret", Role.Editor)
      val entryAId = Entry.create(statementA.id, "content A", new Date(), author.id)
      val entryBId = Entry.create(statementA.id, "content B", new Date(), author.id)

      val loadedStatement = Statement.withEntriesAndRatings(statementA)

      val expectedEntries = Seq(Entry.load(entryAId).get, Entry.load(entryBId).get)

      loadedStatement.entries must containAllOf(expectedEntries)
    }
    "return a statement with all ratings" in new TestStatements {
      Statement.setRating( statementA.id, Rating.PromiseKept )
      Statement.setRating( statementA.id, Rating.InTheWorks )
      Statement.setRating( statementB.id, Rating.PromiseBroken )
      
      val loadedStatement = Statement.withEntriesAndRatings(statementA)

      loadedStatement.ratings.map( _._1 ) must beEqualTo(List(Rating.InTheWorks, Rating.PromiseKept))
    }
  }

  "find" should {
    "return statements matching case-insensitively given text" in new TestStatements {
      val statement = Statement.create("test statement", authorB, catB, Some("some text so test searching"), None)

      Statement.find("some text") must havePair(authorB -> List(statement))
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
    lazy val tagZ = Tag.create("Z tag")
    lazy val tagA = Tag.create("tag A")
    lazy val tagB = Tag.create("tag B")

    override def before: Any = {
      Tag.add(statementA.id, tagZ)
      Tag.add(statementA.id, tagA)
      Tag.add(statementA.id, tagB)
      Tag.add(statementB.id, tagA)
    }
  }

  "tags" should {
    "be ordered alphabetically" in new TaggedTestStatements {
      Statement.load(statementA.id).get.tags.toList must beEqualTo( List(tagA, tagB, tagZ) )
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
    "find statements by rating" in new TestStatements {
      Statement.setRating(statementB.id, Rating.InTheWorks)
      Statement.setRating(statementC.id, Rating.InTheWorks)

      val loadedIds = Statement.byRating(Rating.InTheWorks, None, None).map(_.id)

      loadedIds must containTheSameElementsAs(Seq(statementB, statementC).map(_.id))
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
          Statement.setRating(c, statementA.id, Rating.Stalled)

          Statement.load(statementA.id) must beSome.which(_.rating === Some(Rating.Stalled))
      }
    }

    "return 'true' when given valid id" in new TestStatements {
      DB.withConnection {
        implicit c =>
          val result = Statement.setRating(c, statementA.id, Rating.Stalled)

          result must beEqualTo(true)
      }
    }

    "return 'false' when given invalid id" in {
      DB.withConnection {
        implicit c =>
          val result = Statement.setRating(c, 123, Rating.Stalled)

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
          Statement.edit(c, statementA.id, "new title", catB, Some("test quote"), None)

          Statement.load(statementA.id) must beSome.which(
            stmt => stmt.title === "new title" and
              stmt.category === catB and
              stmt.quote === Some("test quote") 
          )
      }
    }
  }
}