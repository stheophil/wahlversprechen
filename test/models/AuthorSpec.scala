package models

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import helpers.WithTestDatabase

class AuthorSpec extends Specification with WithTestDatabase {

  "load" should {
    "return an author by id" in new Authors {
      Author.load(bob.id) must beSome(bob)
    }

    "return None when given invalid id" in new Authors {
      Author.load(8924) must beNone
    }

    "return an author by name" in new Authors {
      Author.load(bob.name) must beSome(bob)
    }

    "return None when given invalid name" in new Authors {
      Author.load("unknown author") must beNone
    }
  }

  "loadTopLevel" should {
    "return top level author" in {
      Author.create("2nd level author 1", 1, false, "#000000", "#ffffff")
      val expected = Author.create("top level author", 2, true, "#000000", "#ffffff")
      Author.create("2nd level author 2", 3, false, "#000000", "#ffffff")

      Author.loadTopLevel() must beSome(expected)
    }
  }

  "edit" should {
    "persist changes" in new Authors {
      Author.edit(bob.id, "Bob Barker", 5, true, "#00ff00", "#ff00ff")

      Author.load(bob.id) must beSome.which(
        author => author.name === "Bob Barker" and
          author.order === 5 and
          author.top_level === true and
          author.color === "#00ff00" and
          author.background === "#ff00ff"
      )
    }

    "return true when an existing author was edited" in new Authors {
      val result = Author.edit(bob.id, "Bob Barker", 5, true, "#00ff00", "#ff00ff")

      result must beEqualTo(true)
    }

    "return false when editing non-existing author" in {
      val bEdited = Author.edit(4124, "New Political Hope", 5, false, "#00000", "#999999")

      bEdited === false
    }
  }

  "loadAll" should {
    "return empty List when no authors in database" in {
      Author.loadAll() must be empty
    }

    "return all authors" in new Authors {
      Author.loadAll() must containAllOf(allAuthors)
    }
  }

  trait Authors extends Scope {
    val bob = Author.create("Bob", 1, false, "#ffffff", "#000000")
    val alice = Author.create("Alice", 2, true, "#ffffff", "#000000")

    val allAuthors = Seq(bob, alice)
  }

}
