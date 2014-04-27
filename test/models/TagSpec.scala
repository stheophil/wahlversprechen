package models

import org.specs2.mutable.Specification
import helpers.WithTestDatabase
import org.specs2.specification.Scope

class TagSpec extends Specification with WithTestDatabase {
  "load" should {
    "return a tag by its id" in new TestTags {
      Tag.load(foo.id) must beSome(foo)
    }

    "return None when given invalid id" in {
      Tag.load(24234) must beNone
    }

    "return a tag by its name" in new TestTags {
      Tag.load("bar") must beSome(bar)
    }

    "return None when given invalid name" in {
      Tag.load("baz") must beNone
    }
  }

  "loadAll" should {
    "return empty list when no tags in database" in {
      Tag.loadAll() must beEmpty
    }

    "return all stored tags" in new TestTags {
      Tag.loadAll() must containAllOf(allTags)
    }
  }

  "setImportant" should {
    "update a tags 'important' property" in new TestTags {
      Tag.setImportant(foo.id, true)

      Tag.load(foo.id) must beSome.which(_.important)
    }
  }

  "setName" should {
    "change the name of a tag" in new TestTags {
      Tag.setName(foo.id, "foobar")

      Tag.load(foo.id) must beSome.which(_.name === "foobar")
    }

    "merge tags when setting to an existing name" in new TestTags {
      Tag.setName(foo.id, bar.name)

      Tag.load(foo.id) must beNone
    }
  }

  "delete" should {
    "remove a tag from the database" in new TestTags {
      Tag.delete(foo.id)

      Tag.load(foo.id) must beNone
    }
  }

  trait TestTags extends Scope {
    val foo = Tag.create("foo")
    val bar = Tag.create("bar")

    val allTags = Seq(foo, bar)
  }
}
