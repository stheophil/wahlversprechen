package models

import org.specs2.mutable._

import play.api.Play.current
import play.api.db._
import helpers.WithTestDatabase
import java.util.Date
import org.specs2.specification.Scope

class EntrySpec extends Specification with WithTestDatabase {

  trait TestEntries extends Scope {
    val userA = User.create("user.a@test.de", "user a", "secret", Role.Editor)

    val authorA = Author.create("Author A", 1, false, "#ffffff", "#000000")
    val catA = Category.create("category A", 1)
    
    val statementA = Statement.create("Statement A", authorA, catA, None, None, None, None)

    val contentA = "Content in [markdown](http://someurl.com) syntax."
    val entryA = Entry.load( Entry.create(statementA.id, contentA, new Date(), userA.id) ).get
    
    val contentB = """## Different Markdown text
        **bold**
    """
    val entryB = Entry.load(Entry.create(statementA.id, contentB , new Date(), userA.id)).get

    val entries = List(entryB, entryA)
    val latestEntries = List(entryB)
  }

  "loadByStatement" should {
    "return all entries for a statement" in new TestEntries {
      Entry.loadByStatement(statementA.id) must beEqualTo(entries)
    }

    "return empty list of entries for non-existing statement" in {
      Entry.loadByStatement(23722) must beEmpty
    }
  }

  "load" should {
    "return entry by id" in new TestEntries {
      val loadedEntryA = Entry.load(entryA.id)

      loadedEntryA must beSome.which(
        entry => entry === entryA and
          entry.content === contentA and 
          entry.user === userA
      )
    }

    "return None when given invalid id" in {
      Entry.load(4123) must beNone
    }
  }

  "loadRecent" should {
    "return latest entry" in new TestEntries {
      Entry.loadRecent(1) must beEqualTo(latestEntries)
    }

    "return latest entries" in new TestEntries {
      Entry.loadRecent(2) must beEqualTo(entries)
    }
  }

  "contentAsMarkdown" should {
    "return markdown content" in new TestEntries {
      Entry.contentAsMarkdown(entryB.id) must beSome.which(
        content => content === entryB.content
      )
    }
  }

  "delete" should {
    "remove an entry from the database" in new TestEntries {
      val bDeleted = Entry.delete(entryA.id)

      Entry.load(entryA.id) must beNone
      bDeleted must beEqualTo(true)
    }
    "return false when deleting non-existing Entry" in new TestEntries {
      Entry.delete(32454) must beEqualTo(false)
    }
  }

  "edit" should {
    "persist changes" in new TestEntries {
      Entry.edit(entryA.id, "A different content")

      Entry.load(entryA.id) must beSome.which(
        entry => entry.content === "A different content"
      )
    }

    "return true when an existing entry was edited" in new TestEntries {
      val result = Entry.edit(entryA.id, "A different content")
      result === true
    }

    "return false when editing non-existing entry" in {
      val result = Entry.edit(23723, "A different content")
      result === false
    }
  }
}