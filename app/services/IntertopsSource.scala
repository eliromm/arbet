package services

import java.text.SimpleDateFormat
import java.util.TimeZone

import model.Event

import scala.collection.immutable.Seq
import scala.xml._

/**
  * Created by E on 10/09/2016.
  */
object IntertopsSource extends EventService{
  val dateFormat = new SimpleDateFormat("MM/dd/yyy HH:mm:ss")
  dateFormat.setTimeZone(TimeZone.getTimeZone("Etc/GMT+4"))

  def main(args: Array[String]): Unit = {
    getEvents(true).foreach(println)
  }

  override def getEvents(live: Boolean): Seq[Event] = {

    val data = getFeed(live)

    data.flatMap(datas => getMyGames(datas.child)).
      flatMap(res => res._2.map(filterGames).flatMap(nodes => nodes.map(game => {
      getLogger.info(s"game $game")
      val parsedGame = parseMyGame(res._1,game)
      getLogger.info(s"parsed game $parsedGame")
      parsedGame
    }
    ))).flatten

  }

  def parseMyGame(league:String,game: Node): Option[Event] = {

    val names = (game \ "Name").text.split(" v ")

    val lines = (game \ "Bet").filter(t => (t \@ "type").equals("SM")) \ "line"
    if(lines.size>1 && names.length == 2)
    {
      val home = lines.filter(line => (line \@ "name").equals("odds1")).head
      val away = lines.filter(line => (line \@ "name").equals("odds2")).head
      val draw = lines.find(line => (line \@ "name").equals("oddsdraw"))
      Some(Event(dateFormat.parse((game \ "Date").text),
        game \@ "id",
        parseLeague(league),
        names(0),
        names(1),
        BigDecimal(home.text),
        BigDecimal(away.text),
        draw.map(node => BigDecimal(node.text))
      ,getEventSourceName,game.toString()))
      }
    else None

  }

  override def getEventSourceName: String = "intertops"

  def getMyGames(file: NodeSeq): Seq[(String, NodeSeq)] = {
    val nodeSeq = file \ "Competition"
    nodeSeq.map(node => {
      val league = node \@ "compno"
      (league,node \ "Match")
    })
  }

  override def filterGames(file: NodeSeq): NodeSeq = file
}
