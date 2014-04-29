package models

import org.specs2.mutable.Specification
import helpers.WithTestDatabase
import org.specs2.specification.Scope

class UserSpec extends Specification with WithTestDatabase {

  "findAll" should {
    "return empty List when no user stored" in {
      User.findAll must beEmpty
    }

    "return all stored users" in new TestUsers {
      User.findAll() must containAllOf(allUsers)
    }
  }

  "load" should {
    "return a user by its id" in new TestUsers {
      User.load(bob.id) must beSome(bob)
    }

    "return None when user not found by id" in new TestUsers {
      User.load(987123) must beNone
    }

    "return a user by its email" in new TestUsers {
      User.load(bob.email) must beSome(bob)
    }

    "return None when user not found by email" in {
      User.load("invalid.email@some-provider.com") must beNone
    }
  }

  "authenticate" should {
    "return the user when given correct credentials" in new TestUsers {
      User.authenticate(bob.email, "bobsPassword") must beSome(bob)
    }

    "return None when given incorrect password" in new TestUsers {
      User.authenticate(bob.email, "wrong password") must beNone
    }

    "return None when given incorrect email" in new TestUsers {
      User.authenticate("wrong.email@test.de", "bobsPassword") must beNone
    }

  }

  "delete" should {
    "remove a user from the database" in new TestUsers {
      User.delete(bob.id)

      User.load(bob.id) must beNone
    }
  }

  "create" should {
    "not store password as plain text" in {
      val bob: User = models.User.create("bob@test.de", "bob", "plainPassword", Role.Admin)

      bob.password must not be equalTo("plainPassword")
    }
  }

  "edit" should {
    "persist changes" in new TestUsers {
      User.edit(bob.id, "bobs.other.email@test.de", "Bob", role = Some(Role.Editor))

      User.load(bob.id) must beSome.which(
        user => user.email === "bobs.other.email@test.de" and
          user.name === "Bob" and
          user.role === Role.Editor
      )
    }

    "assign new salt when changing password" in new TestUsers {
      User.edit(bob.id, bob.email, bob.name, password = Some("newPassword"))

      User.load(bob.id) must beSome.which(
        user => {
          user.password must not be equalTo(bob.password)
          user.salt must not be equalTo (bob.salt)
        }
      )
    }
  }

  trait TestUsers extends Scope {
    val bob: User = models.User.create("bob@test.de", "bob", "bobsPassword", Role.Admin)
    val alice: User = models.User.create("alice@test.de", "alice", "alicesPassword", Role.Editor)

    val allUsers = Seq(alice, bob)
  }

}