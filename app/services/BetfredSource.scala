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
      "FOOTBALL-UEFA-cup.XML",
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
      val events = getGames(file)
      events.map(event => (filterGames(event \ "bettype"), parseDate(event \@ "date", event \@ "time"))).flatMap(nodes => nodes._1.map(game => {
        getLogger.info(s"game $game")
        val parsedGame = parseMyGame(game, league, nodes._2)
        getLogger.info(s"parsed game $parsedGame")
        parsedGame
      }
      ))
    })).flatten
  }

  def parseMyGame(bettype: Node, league: String, resultDate: Date): Option[Event] = {
    try {
      val bets = bettype \ "bet"
      val home = bets.filter(bet => bet \@ "had-value" == "HOME").head
      val away = bets.filter(bet => bet \@ "had-value" == "AWAY").head
      val draw = bets.find(bet => bet \@ "had-value" == "DRAW")

      val event = Event(resultDate,
        bettype \@ "bettypeid",
        parseLeague(league),
        home \@ "name",
        away \@ "name",
        BigDecimal(home \@ "priceDecimal"),
        BigDecimal(away \@ "priceDecimal"),
        draw.map(drawValue => BigDecimal(drawValue \@ "priceDecimal")),
        getEventSourceName, bettype.toString())

      Some(event)

    }
    catch {
      case t :Throwable => getLogger.error(s"error parsing ${bettype.toString()}",t)
        None
    }
  }

  private def parseDate(dateVal: String, timeVal: String) = {
    Try {
      dateFormat.parse(dateVal + " - " + timeVal)
    }.getOrElse(new Date(0))
  }

  override def getEventSourceName: String = "betfred"

  override def getGames(file: NodeSeq): NodeSeq = {
    file \ "event"
  }

  override def filterGames(file: NodeSeq): NodeSeq = {
    file.filter(bettype => (bettype \@ "name") == "Match Result" && (bettype \@ "inrunning") == "0")
  }
}
