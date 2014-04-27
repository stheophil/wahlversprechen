package models

import org.specs2.mutable.Specification
import helpers.WithTestDatabase

class CategorySpec extends Specification with WithTestDatabase {
  "loadAll" should {
    "return all persistent categories" in {
      val catC = Category.create("Category C", 3)
      val catA = Category.create("Category A", 1)
      val catB = Category.create("Category B", 2)

      Category.loadAll() must be equalTo List(catA, catB, catC)
    }
  }
}
