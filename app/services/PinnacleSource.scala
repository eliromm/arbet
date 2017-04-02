package services

import java.text.SimpleDateFormat
import java.util.TimeZone

import model.{Event, League}
import play.api.Logger

import scala.xml.{Elem, Node, NodeSeq, XML}

/**http://odds.smarkets.com/oddsfeed.xml
  * Created by E on 10/09/2016.
  */
object PinnacleSource extends EventService{

  override def getLogger :Logger = Logger("pinnacle")

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  dateFormat.setTimeZone(TimeZone.getTimeZone("Etc/GMT"))

  override def loadUrl() = Seq(getUrl("http://xml.pinnaclesports.com/pinnacleFeed.aspx"))


  override def getEventSourceName: String = "pinnacle"

  override def parseGame(event: Node): Option[Event] = {

   Some( Event(dateFormat.parse((event \ "event_datetimeGMT").text),
      Integer.valueOf((event \ "gamenumber").text),
      parseLeague((event \ "league").text),
      ((event \ "participants" \ "participant").filter(participant =>(participant \ "visiting_home_draw").head.text.equals("Home")) \ "participant_name").text,
      ((event \ "participants" \ "participant").filter(participant =>(participant \ "visiting_home_draw").head.text.equals("Visiting")) \ "participant_name").text,
      toDecimal(Integer.valueOf((event \ "periods" \ "period").headOption.map(period => (period \ "moneyline" \ "moneyline_home").text).filter(_.nonEmpty).getOrElse("1")) ),
      toDecimal(Integer.valueOf((event \ "periods" \ "period").headOption.map(period => (period \ "moneyline" \ "moneyline_visiting").text).filter(_.nonEmpty).getOrElse("1"))),
      (event \ "periods" \ "period").headOption.map(period => (period \ "moneyline" \ "moneyline_draw").text).filter(_.nonEmpty).map(line => toDecimal(Integer.valueOf(line)))
    ,getEventSourceName,event.toString()))}

//Serie A
  override def getGames(file: NodeSeq): NodeSeq = {
    file \ "events" \ "event"
  }

  override def filterGames(file: NodeSeq): NodeSeq = file.filter(
    event =>
      //        (event \"sporttype").text.equals("Tennis") &&
      (event \ "periods" \ "period" \ "period_number").text.equals("0") &&
        filterName((event \ "participants" \ "participant").text)
  )

  def main(args: Array[String]): Unit = {
    getEvents(true)
  }
}