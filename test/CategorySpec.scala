package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

import models._

 class CategorySpec extends Specification with WithFilledTestDatabase  {

  "Category" should {
    "return all records" in {
      Category.loadAll().size must beEqualTo(2)
    }
    "create new category" in {
      val categories = Category.loadAll()
      val catCreated = Category.create("Department of Labor", categories.map(_.order).min - 1)
      
      Category.loadAll() must beEqualTo(catCreated :: categories)
    }
  }  
}