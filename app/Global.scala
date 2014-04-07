import play.api._
import play.api.templates._

import models._
import anorm._

object Global extends GlobalSettings {  
  override def onStart(app: Application) {
    InitialData.insert(app)
  }  
}

object InitialData {
  
  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)
  
  def insert(implicit app: Application) = {
    if(User.findAll.isEmpty) {
      // TODO: Replace these default values with useful configuration options or 
      // a setup page
      var ausers = new collection.mutable.ArrayBuffer[User];
      Array(
          ("sebastian@theophil.net", "Sebastian", "secret", Role.Admin),
          ("test@test.net", "Tester", "secret", Role.Editor),
          ("test2@test.net", "Tester2", "secret", Role.Editor)
      ).foreach(t =>
        ausers += User.create(t._1, t._2, t._3, t._4)
      );

      var aauthors = new collection.mutable.ArrayBuffer[Author];
      Array( ("Koalitionsvertrag", 1, true, "#ffffff", "#999999"), ("CDU Wahlprogramm", 2, false, "#ffffff", "#000000"), ("FDP Wahlprogramm", 3, false, "#444", "#FFE500") ).foreach( 
        { case (name, order, rated, color, background) => { 
          aauthors += Author.create(name, order, rated, color, background)
        }}) 
    }    
  }
  
}