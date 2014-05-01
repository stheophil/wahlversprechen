package controllers

import org.specs2.mutable.Specification
import helpers.WithTestDatabase
import models.{User, Role}
import play.api.test._
import play.api.test.Helpers._
import org.specs2.specification.Scope

class AdminSpec extends Specification with WithTestDatabase {

  trait ValidUser extends Scope {
    val user = User.create("test@test.de", "test", "secret", Role.Admin)
  }

  "login" should {
    "authenticate a user on valid credentials" in new ValidUser {
      val Some(site) = route(FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
          "email" -> "test@test.de",
          "password" -> "secret"
        ))

      session(site).get("email") must beSome("test@test.de")
      redirectLocation(site) must beSome("/")
    }

    "authenticate a user on invalid wrong password" in new ValidUser {
      val Some(site) = route(FakeRequest(POST, "/login")
        .withFormUrlEncodedBody(
          "email" -> "test@test.de",
          "password" -> "wrong password"
        ))

      session(site).get("email") must beNone
      status(site) must beEqualTo(BAD_REQUEST)
    }
  }
}
