package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
 class UserSpec extends Specification with WithTestDatabase  {

  "models.User" should {
    "return all records" in {
      models.User.findAll().size must beEqualTo(2)
    }
    "return record by id" in {
      val user = models.User.findAll().head
      models.User.load(user.id) must beEqualTo(Some(user))
    }
    "return record by email" in {
      val user = models.User.findAll().head
      models.User.load(user.email) must beEqualTo(Some(user))
    }
    "create authenticatable user with hashed password" in {
      val user = models.User.create("anothertest@test.de", "Test User", "password", models.Role.Editor)
      val userLoaded = models.User.load(user.id)
      val userAuthenticate = models.User.authenticate("anothertest@test.de", "password")

      (models.User.findAll().size === 3) and
      (userLoaded === Some(user)) and
      (user.password !== "password") and
      (user.salt.length === 20) and
      (userAuthenticate === Some(user))
    }
    "assign new salt when editing user" in {
      val user = models.User.create("anothertest@test.de", "Test User", "password", models.Role.Editor)
      val userEdited = models.User(user.id, "anothertest2@test.de", "Another Name", "password", "", models.Role.Admin)
      models.User.edit(userEdited.id, userEdited.email, userEdited.name, Some(userEdited.password), Some(userEdited.role))

      val userLoaded = models.User.load(user.id)

      (models.User.findAll().size === 3) and
      (userLoaded must beSome) and
      (userLoaded.get.name === userEdited.name) and
      (userLoaded.get.email === userEdited.email) and
      (userLoaded.get.role === userEdited.role) and
      (user.password !== userLoaded.get.password) and
      (user.salt !== userLoaded.get.salt) and
      (models.User.authenticate(userEdited.email, userEdited.password) === userLoaded)
    }
  }  
}