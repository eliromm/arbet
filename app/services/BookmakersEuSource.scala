package services

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import model.Event

import scala.util.Try
import scala.xml.{Node, NodeSeq}

/**
  * Created by E on 10/09/2016.
  */
object BookmakersEuSource extends EventService{

  val dateFormat = new SimpleDateFormat("yyyyMMdd - HH:mm:ss")
  dateFormat.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"))

  def main(args: Array[String]): Unit = {
    getEvents(true).foreach(println)
  }

  override def parseGame(game: Node): Option[Event] = {

    val line = (game \ "line").head

    try {
      val resultDate = Try {
        dateFormat.parse((game \@ "gmdt") + " - " + (game \@ "gmtm"))
      }.getOrElse(new Date(0))

      val event = Event(resultDate, game \@"idgm", parseLeague(game \@ "idlg"), game \@ "htm", game \@ "vtm",
        toDecimal((line \@ "hoddst").toInt), toDecimal((line \@ "voddst").toInt),
        line.attribute("vspoddst").filter(_.head.head.text.nonEmpty).map(node => toDecimal(node.head.text.toInt)),
        getEventSourceName,game.toString())
      Some(event)

    } catch {

      case e:Throwable  =>
        None
    }
  }

  override def getEventSourceName: String = "bookmakerseu"

  override def getGames(file: NodeSeq): NodeSeq = file \ "Leagues" \ "league" \ "game"

  override def filterGames(file: NodeSeq): NodeSeq = {
    file.filter(node => (node \@ "gpd") == "Game" && (node \@ "htm") != "No" && (node \@ "htm") != "Yes")
  }
}
