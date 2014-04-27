package helpers

import org.specs2.specification._
import org.specs2.execute.AsResult
import play.api.test.Helpers._
import play.api.test.FakeApplication
import org.apache.commons.io.FileUtils
import play.api.db.DB
import scala.collection.immutable.TreeSet

// Taken from http://workwithplay.com/blog/2013/06/19/integration-testing/
trait WithTestDatabase extends AroundExample {

  val testDb = Map(
    "db.default.url" -> "jdbc:postgresql://localhost/wahlversprechen_test"
//    "logger.application" -> "ERROR",
//    "logger.play" -> "ERROR"
  )

  def around[T: AsResult](t: => T) = {
    val app = FakeApplication(additionalConfiguration = testDb)
    running(app) {
      new SchemaManager(app).dropCreateSchema
      app.global.onStart(app)
      AsResult(t)
    }
  }

  private class SchemaManager(val app: FakeApplication) {

    private val evolutions = parseEvolutions

    private def parseEvolutions = {
      import scala.collection.JavaConversions._
      val evolutions = FileUtils.listFiles(app.getFile("conf/evolutions/default/"), Array("sql"), false)
      evolutions.map { evolution =>
        val evolutionContent = FileUtils.readFileToString(evolution)
        val splittedEvolutionContent = evolutionContent.split("# --- !Ups")
        val upsDowns = splittedEvolutionContent(1).split("# --- !Downs")
        val index = evolution.getName().replace(".sql", "").toInt
        new Evolution(index, upsDowns(0), upsDowns(1))
      }
    }

    def dropCreateSchema {
      dropSchema
      createSchema
    }

    private def dropSchema = orderEvolutions(new EvolutionsOrderingDesc).foreach { _.runDown(app) }

    private def createSchema = orderEvolutions(new EvolutionsOrderingAsc).foreach { _.runUp(app) }

    private def orderEvolutions(ordering: Ordering[Evolution]) = {
      evolutions.foldLeft(new TreeSet[Evolution]()(ordering)) { (treeSet, evolution) =>
        treeSet + evolution
      }
    }
  }

  private class EvolutionsOrderingDesc extends Ordering[Evolution] {
    override def compare(a: Evolution, b: Evolution): Int = b.index compare a.index
  }

  private class EvolutionsOrderingAsc extends Ordering[Evolution] {
    def compare(a: Evolution, b: Evolution) = a.index compare b.index
  }

  private case class Evolution(val index: Int, up: String, down: String) {

    private val upQueries = up.trim.split(";")
    private val downQueries = down.trim.split(";")

    def runUp(implicit app: FakeApplication) = {
      runQueries(upQueries)
    }

    def runDown(implicit app: FakeApplication) = {
      runQueries(downQueries)
    }

    private def runQueries(queries: Array[String])(implicit app: FakeApplication) {
      DB.withTransaction { conn =>
        queries.foreach { q =>
          conn.createStatement.execute(q)
        }
      }
    }
  }
}

trait WithFilledTestDatabase extends WithTestDatabase {
  val sheet2 = "12ooSGJrHN6l3mvwMsA10fEetYhIIjLA_ZL57pXxJSxc"
  val sheet3 = "1DJbUAIz33ogxR_bhgiXTFYdE4z1-QkYvLSTltz1Yd24"
  val sheet4 = "1us0DafZsza8zH3mG8cYZNTPnl-iM8g2WXMzU1-h4an4"

  override def around[T: AsResult](t: => T) = {
    // TODO: Load data locally and not from Google
    super.around{
      controllers.Import.loadSpreadSheet("Funny Party", sheet2)
      controllers.Import.loadSpreadSheet("Serious Party", sheet3)
      controllers.Import.loadSpreadSheet("Coalition Treaty", sheet4)
      t
    }
  }
}
