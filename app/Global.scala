import java.util.concurrent.TimeUnit
import play.api._
import play.api.templates._
import play.api.libs.concurrent._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _ => {
        Logger.info("Scheduling the feed parser daemon")
        Akka.system(app).scheduler.schedule(
          Duration.Zero,
          Duration(2, "hours"),
          new Runnable {
            def run() {
              controllers.FeedDaemon.update()
            }
          })

        InitialData.insert(app)
      }
    }
  }
}

object InitialData {

  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

  def insert(implicit app: Application) = {
    if (User.findAll.isEmpty) {
      // TODO: Replace these default values with useful configuration options or 
      // a setup page
      var ausers = new collection.mutable.ArrayBuffer[User];
      Array(
        ("test@test.net", "Tester", "secret", Role.Admin),
        ("test2@test.net", "Tester2", "secret", Role.Editor)
      ).foreach(t =>
        ausers += User.create(t._1, t._2, t._3, t._4)
        );

      var aauthors = new collection.mutable.ArrayBuffer[Author];
      Array(
        ("Coalition Treaty", 1, true, "#ffffff", "#999999"),
        ("Funny Party", 2, false, "#ffffff", "#000000"),
        ("Serious Party", 3, false, "#444", "#FFE500")
      ).foreach(
      {
        case (name, order, rated, color, background) => {
          aauthors += Author.create(name, order, rated, color, background)
        }
      })
    }
  }

}
