package services

import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.{Files, Paths}
import java.util.{Calendar, Date}

import model.Event
import play.api.{Logger, Play}
import model.League

import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.xml._
import play.api.Play.current

/**
  * Created by E on 10/09/2016.
  */
abstract class EventService {

  implicit def leagueToString(league :League.EnumVal):String = {
    league.toString
  }

  val filterTeamNames = Seq("(map","Corners","Bookings","+","Score","Game","Matches")

  def getLogger: Logger = Logger(getEventSourceName)

  def filterName(name:String):Boolean={
    filterTeamNames.forall(!name.contains(_))
  }

  def toDecimal(line:Int):BigDecimal={
    if(line>0 ) BigDecimal(line+100)/100
    else BigDecimal(100-line)/(-line)
  }

  def getEventsk(live : Boolean=false):Seq[Event] = {
   val buffer = ListBuffer[Event]()
    for (i <-  1 to 100){
      buffer += Event(new Date(),1,"","","",1,1,None,"test","")
   }

    buffer
  }

  def getUrl(url:String): Elem = {
    scala.xml.XML.load(url)
  }

  def randomPayback: BigDecimal = {
    Random.nextFloat() * 2.3
  }


  def getEvents(live: Boolean): Seq[Event] = {
    val feed = getFeed(live)


    feed.map(datas => getGames(datas.child)).map(filterGames).flatMap(nodes => nodes.map(game=> {
      getLogger.info(s"game $game")
      val parsedGame = parseGame(game)
      getLogger.info(s"parsed game $parsedGame")
      parsedGame
    }
    )).flatten
  }

  def getFeed(live: Boolean): Elem = {
    val fileName = Play.configuration.getString("online_data.path").getOrElse(".") + "/" + this.getClass.getSimpleName + "_data.xml"
    val data = if (live || localDataExpired(fileName)) {
      getLogger.info(s"using online data for ${this.getClass.getSimpleName}")
      val loadedData = Elem(null, "main", Null, TopScope, false, loadUrl(): _*)

      XML.save(fileName, loadedData, "UTF-8")
      loadedData
    }
    else {
      getLogger.info(s"using old data for ${this.getClass.getSimpleName}")
      XML.loadFile(fileName)
    }
    data
  }

  def localDataExpired(fileName: String): Boolean = {
    try {
      val millis = Files.getFileAttributeView(
        Paths.get(fileName),
        classOf[BasicFileAttributeView] //corrected
      ).readAttributes().lastModifiedTime().toMillis

      val lastHour = Calendar.getInstance()
      lastHour.add(Calendar.HOUR, -1 * Play.configuration.getInt("online_data.revalidate_after").getOrElse(1) )
      lastHour.getTime.after(new Date(millis))
    } catch {
      case e: Exception =>
        getLogger.warn("error getting last data update timestamp")
        // file probably  unreadable so data is expired
        true
    }
  }


  def parseLeague(name: String): League.EnumVal = {
    League(Play.configuration.getString(s"$getEventSourceName.leagues.$name").getOrElse(name))
  }

  def loadUrl() = Seq(getUrl(Play.configuration.getString(s"$getEventSourceName.url").getOrElse("")))

  def getEventSourceName : String
  def parseGame(bettype: Node) : Option[Event]= ???
  def getGames(file: NodeSeq): NodeSeq = ???
  def filterGames(file: NodeSeq): NodeSeq= ???
}
