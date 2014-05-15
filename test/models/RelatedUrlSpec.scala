package models

import org.specs2.specification.Scope
import org.specs2.mutable.Specification
import helpers.WithTestDatabase
import helpers.DateFactory._
import java.util.Date

class RelatedUrlSpec  extends Specification with WithTestDatabase {

  trait RelatedUrls extends Scope {
    val userA = User.create("user.a@test.de", "user a", "secret", Role.Editor)

    val authorA = Author.create("Author A", 1, false, "#ffffff", "#000000")
    val catA = Category.create("category A", 1)

    val statementA = Statement.create("Statement A", authorA, catA, None, None)

    val url1 = RelatedUrl.create(statementA.id, "A web article", "http://someurl.com", 9.6, 2013 \ 12 \ 1, RelatedCategory.Article)
    val url2 = RelatedUrl.create(statementA.id, "Another web article", "http://someurl2.com", 11, 2013 \ 11 \ 1, RelatedCategory.Article)

    val allRelatedUrls = List(url2, url1)
  }

  "loadByUrl" should {
    "return a related url by id and url" in new RelatedUrls {
      val loaded = RelatedUrl.loadByUrl(statementA.id, url1.url)
      loaded must beSome.which {
        url =>
          url.copy( lastseen = url1.lastseen ) must beEqualTo(url1)
          url.lastseen.getTime must beEqualTo(url1.lastseen.getTime)
      }
    }

    "return nothing for non-existing url" in new RelatedUrls {
      RelatedUrl.loadByUrl(statementA.id, "...") must beNone
    }

    "return nothing for non-existing statement.id" in new RelatedUrls {
      RelatedUrl.loadByUrl(24393, url1.url) must beNone
    }
  }

  "load" should {
    "return all urls for a statement" in new RelatedUrls {
      RelatedUrl.load(statementA.id).map(_.id) must beEqualTo(allRelatedUrls.map(_.id))
    }
  }

  "update" should {
    "update date" in new RelatedUrls {
      val date : Date = 2014 \ 1 \ 1
      val result = RelatedUrl.update(url1.id, date)

      result must beTrue
      val loaded = RelatedUrl.loadByUrl(statementA.id, url1.url) must beSome.which {
        url =>
          url.lastseen.getTime must beEqualTo(date.getTime)
          url.copy(lastseen = url1.lastseen) must beEqualTo(url1)
      }
    }
  }
}
