package services

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import model.Event
import play.api.Play
import play.api.Play.current

import scala.util.Try
import scala.xml._

/**
  * Created by E on 10/09/2016.
  */
object BetfredSource extends EventService {

  val dateFormat = new SimpleDateFormat("yyyyMMdd - HHmm")
  dateFormat.setTimeZone(TimeZone.getTimeZone("Etc/GMT"))

  override def loadUrl(): List[Elem] = {
    val names = List("Football-Champions-League.xml",
      "Football-german-bundesliga.xml",
      "Football-Bundesliga_2.xml",
      "FOOTBALL-DUTCH-EREDIVISIE.XML",
      "FOOTBALL-italian-serie-a.XML",
      "Football-Premiership.xml",
      "Football-Premiership.xml",
      "Football-spanish-primera.xml",
      "Tennis-ATP.xml",
      "Tennis-wta.xml"
    )
    names.map(name => {
      getUrl(Play.configuration.getString("betfred.url").getOrElse("") + name)
    })
  }

  def main(args: Array[String]): Unit = {
    getEvents(false).foreach(println)
  }

  override def getEvents(live: Boolean): Seq[Event] = {
    val data = getFeed(live)

    data.flatMap(datas => datas.child.flatMap(file => {
      val league = file \@ "name"
      getGames(file).map(filterGames).flatMap(nodes => nodes.map(game => {
        getLogger.info(s"game $game")
        val parsedGame = parseMyGame(game, league)
        getLogger.info(s"parsed game $parsedGame")
        parsedGame
      }
      ))
    })).flatten
  }

  def parseMyGame(bettype: Node, league: String): Option[Event] = {

    val bets = bettype \ "bet"
    val home = bets.filter(bet => bet \@ "had-value" == "HOME").head
    val away = bets.filter(bet => bet \@ "had-value" == "AWAY").head
    val draw = bets.find(bet => bet \@ "had-value" == "DRAW")

    val resultDate = Try {
      dateFormat.parse((bettype \@ "bet-start-date") + " - " + (bettype \@ "bet-start-time"))
    }.getOrElse(new Date(0))

    val event = Event(resultDate,
      1,
      parseLeague(league),
      home \@ "name",
      away \@ "name",
      BigDecimal(home \@ "priceDecimal"),
      BigDecimal(away \@ "priceDecimal"),
      draw.map(drawValue => BigDecimal(drawValue \@ "priceDecimal")),
      getEventSourceName, bettype.toString())

    Some(event)
  }

  override def getEventSourceName: String = "betfred"

  override def getGames(file: NodeSeq): NodeSeq = {
    file \ "event" \ "bettype"
  }

  override def filterGames(file: NodeSeq): NodeSeq = {
    file.filter(bettype => (bettype \@ "name") == "Match Result")
  }
}
