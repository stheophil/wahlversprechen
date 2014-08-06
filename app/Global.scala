import java.util.concurrent.TimeUnit
import play.api._
import play.api.templates._
import play.api.libs.concurrent._
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

import models._

object Global  extends WithFilters(new GzipFilter) with GlobalSettings {
  val scheduler = new java.util.concurrent.ScheduledThreadPoolExecutor(1)

  override def onStart(app: Application) {
    play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _ => {
        Logger.info("Scheduling the feed parser daemon")
        
        scheduler.scheduleAtFixedRate( 
          new Runnable {
            def run() {
              Logger.info("Started FeedDaemon.update()")
              try {
                controllers.FeedDaemon.update()  
              } catch {
                case e: InterruptedException => 
                  Logger.info("Interrupted FeedDaemon.update()")
                case e: Exception =>
                  Logger.error("Unknown Exception in FeedDaemon.update()")
                  e.printStackTrace()
                  throw e
              } finally {
                Logger.info("Finished FeedDaemon.update()")
              }             
            }
          }, 0, 2, TimeUnit.HOURS
        )

        InitialData.insert(app)
      }
    }
  }

  override def onStop(app: Application) {
    Logger.info("Shutdown ScheduledThreadPoolExecutor")
    scheduler.shutdownNow()

    try {
      if(scheduler.awaitTermination(2, TimeUnit.MINUTES)) {
        Logger.info("scheduler.awaitTermination succeeded")
      } else {
        Logger.error("ScheduledThreadPoolExecutor didn't terminate before timeout")
      }
    } catch {
      case e: Exception => 
        Logger.error("scheduler.awaitTermination was interrupted")
        e.printStackTrace
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
        case (name, order, top_level, color, background) => {
          aauthors += Author.create(name, order, top_level, color, background)
        }
      })
    }
  }

}
