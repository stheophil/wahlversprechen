import play.api._
import play.api.templates._

import models._
import anorm._

object Global extends GlobalSettings {  
  override def onStart(app: Application) {
    InitialData.insert()
  }  
}

/**
 * Initial set of data to be imported 
 * in the sample application.
 */
object InitialData {
  
  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)
  
  def insert() = {    
    if(User.findAll.isEmpty) {
      var ausers = new collection.mutable.ArrayBuffer[User];
      Array(
          ("sebastian@theophil.net", "Sebastian", "secret", Role.Admin),
          ("test@test.net", "Tester", "secret", Role.Editor),
          ("test2@test.net", "Tester2", "secret", Role.Editor)
      ).foreach(t =>
        ausers += User.create(t._1, t._2, t._3, t._4)
      );
      
      var acategories = new collection.mutable.ArrayBuffer[Category];
      Array( ("Wirtschaft", 1), ("Inneres", 2), ("Verteidigung", 3), ("Landwirtschaft", 4) ).foreach(t => 
        acategories += Category.create(t._1, t._2)
      )
                
      var astmt = new collection.mutable.ArrayBuffer[Statement];
      Array(
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), Rating.PromiseKept),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), Rating.PromiseKept),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), Rating.PromiseBroken),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), Rating.Unrated),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), Rating.Compromise),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), Rating.PromiseBroken),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), Rating.InTheWorks),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), Rating.PromiseKept),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), Rating.PromiseBroken),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(2), Rating.PromiseKept),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(2), Rating.Stalled),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), Rating.Stalled),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), Rating.PromiseKept),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), Rating.PromiseKept),
        ("Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), Rating.PromiseKept)
      ).foreach(t => 
        astmt += Statement.create(t._1, t._2, t._3)
      )

      val aentries = List(
        ("<strong>This is a super important update.</strong>", date("2013-05-10"), ausers(0)),
        ("<p>Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.</p>", date("2013-05-12"), ausers(1)), 
        ("<i>Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.</i>", date("2013-05-09"), ausers(2)),
        ("<h1>This is a super important update</h1><p>Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata</p>", date("2013-05-20"), ausers(1)),
        ("<blockquote>\"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore\"</blockquote>", date("2013-05-21"), ausers(0))
      )

      def addEntries(stmt: Statement, entries: List[(String, java.util.Date, User)]) {
          entries.foreach( t=>
            Entry.create( stmt.id, t._1, t._2, t._3.id)
          )
      }

      addEntries( astmt(0), aentries.slice(0, 2) )
      addEntries( astmt(1), aentries.slice(1, 3) )
      addEntries( astmt(2), aentries )
      addEntries( astmt(3), aentries.slice(0, 4) )
    }    
  }
  
}