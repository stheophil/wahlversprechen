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

  "edit" should {
    "persist changes" in new Authors {
      Author.edit(bob.id, "Bob Barker", 5, true, "#00ff00", "#ff00ff")

      Author.load(bob.id) must beSome.which(
        user => user.name === "Bob Barker" and
          user.order === 5 and
          user.rated === true and
          user.color === "#00ff00" and
          user.background === "#ff00ff"
      )
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
