package services

import java.text.SimpleDateFormat
import java.util.TimeZone

import model.{Event, League}
import play.api.{Logger, Play}
import play.api.Play.current

import scala.collection.immutable.Seq
import scala.xml._

/**
  * Created by E on 10/09/2016.
  */
object BetclicSource extends EventService {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  dateFormat.setTimeZone(TimeZone.getTimeZone("utc"))

  def main(args: Array[String]): Unit = {
    getEvents(false)
  }

  override def getEvents(live: Boolean): Seq[Event] = {
    val data = getFeed(live)

    data.flatMap(datas => {
      getMyGames(datas.child).flatMap(res => {
        res._2.map(filterGames).flatMap(nodes => nodes.map(game => {
          getLogger.info(s"game $game")
          val parsedGame = parseMyGame(game, res._1)
          getLogger.info(s"parsed game $parsedGame")
          parsedGame
        }
        ))
      })
    }).flatten
  }

  def parseMyGame(game: Node, league: String): Option[Event] = {

    try {
      getLogger.info(s"match $game")
      val bets = ((game \ "bets" \ "bet").filter(bet => {
        val name = bet \@ "name"
        name.equals("Match Winner") || name.equals("Match Result")
      }) \ "choice").map(bet =>
        (BigDecimal(bet \@ "odd"), bet \@ "name"))

      val names = (game \@ "name").split('-').map(_.trim)

      if (bets.nonEmpty && names.length == 2) {
        val date = dateFormat.parse(game \@ "start_date")

        val parsedEvent = Event(date, 1, parseLeague(league), names(0), names(1),
          bets.find(_._2.equals("%1%")).get._1, bets.find(_._2.equals("%2%")).get._1,
          bets.find(_._2.equals("Draw")).map(_._1), getEventSourceName, game.toString())

        getLogger.info(s"event $parsedEvent")

        Some(parsedEvent)
      }
      else None
    }
    catch {
      case e: Exception =>
        getLogger.warn(s"erorr parsing $game", e)
        None
    }
  }

  override def getEventSourceName = "betclic"

  def getMyGames(file: NodeSeq): Seq[(String, NodeSeq)] = {
    val nodeSeq = file \ "sport" \ "event"
    nodeSeq.map(node => {
      val league = node \@ "id"
      (league, node \ "match")
    })

  }

  override def filterGames(file: NodeSeq): NodeSeq = file
}
