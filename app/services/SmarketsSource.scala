package services

import java.io.BufferedInputStream
import java.net.URL
import java.nio.charset.CodingErrorAction
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.zip.GZIPInputStream

import model.Event

import scala.io.{Codec, Source}
import scala.xml.{Elem, Node, NodeSeq}

/**
  * Created by E on 10/09/2016.
  */
object SmarketsSource extends EventService {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  dateFormat.setTimeZone(TimeZone.getTimeZone("Etc/GMT"))

  override def getUrl(url: String): Elem = {
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)
    val lines = Source.fromInputStream(new GZIPInputStream(new BufferedInputStream(new URL(url).openStream()))).mkString

    scala.xml.XML.loadString(lines)

  }

  override def parseGame(event: Node): Option[Event] = {
    val marketNode = (event \ "market").find(node => (node \@ "slug") == "winner" && node.child.nonEmpty)

    marketNode.flatMap(market => {
      try {
        val date = (event \@ "date") + " " + (event \@ "time")
        val home = (market \ "contract").filter(contract => (contract \@ "slug").equals("home"))
        val away = (market \ "contract").filter(contract => (contract \@ "slug").equals("away"))
        val draw = (market \ "contract").filter(contract => (contract \@ "slug").equals("draw"))

        Some(Event(dateFormat.parse(date),
          Integer.valueOf(event \@ "id"),
          parseLeague(event \@ "parent"),

          home \@ "name",
          away \@ "name",

          BigDecimal((home \ "offers" \ "price").head.\@("decimal")),
          BigDecimal((away \ "offers" \ "price").head.\@("decimal")),
          Some(BigDecimal((draw \ "offers" \ "price").head.\@("decimal")))
          , getEventSourceName, event.toString()))
      }
      catch {
        case e: Exception => getLogger.warn("error parsing", e)
          None
      }
    }
    )
  }

  override def getEventSourceName: String = "smarkets"

  override def getGames(file: NodeSeq): NodeSeq = {
    file \ "event"
  }

  override def filterGames(file: NodeSeq): NodeSeq = file.filter(
    event =>
      (event \@ "type") == "Football match"

  )

  def main(args: Array[String]): Unit = {
    //    getEvents(true)

    //    val result = Await.result(wsClient.url("http://odds.smarkets.com/oddsfeed.xml").get(), Duration(10, TimeUnit.SECONDS))
    //        println(result.body)
    //    val load = scala.xml.XML.load(stream)
    //    val stream = fromInputStream(value.openStream())
    val lines = Source.fromInputStream(new GZIPInputStream(new BufferedInputStream(new URL("http://odds.smarkets.com/oddsfeed.xml").openStream()))).mkString
    println(lines)
  }
}