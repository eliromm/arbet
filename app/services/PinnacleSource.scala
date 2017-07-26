package services

import java.text.SimpleDateFormat
import java.util.logging.Level
import java.util.{Calendar, TimeZone}

import com.gargoylesoftware.htmlunit.html._
import model.Event


/**
  * Created by e on 18/06/17.
  */
object PinnacleSource extends EventService {
  val dateFormatNoYear = new SimpleDateFormat("E dd/MM")
  val timeFormat = new SimpleDateFormat("HH.mm")

  override def getEvents(live: Boolean): Seq[Event] = {
    import scala.collection.JavaConverters._
    val urls = List("https://www.pinnacle.com/en/odds/match/soccer/england/england-premier-league"
      , "https://www.pinnacle.com/en/odds/match/soccer/germany/bundesliga"
      , "https://www.pinnacle.com/en/odds/match/soccer/uefa/uefa-champions-league-qualifiers"
      , "https://www.pinnacle.com/en/odds/match/soccer/uefa/uefa-europa-league"
      , "https://www.pinnacle.com/en/odds/match/soccer/uefa/uefa-europa-league-qualifiers"

    )

    val timeout = 5000

    import com.gargoylesoftware.htmlunit.{BrowserVersion, WebClient}
    val client = new WebClient(BrowserVersion.CHROME)

    client.getOptions.setJavaScriptEnabled(true)
    client.getOptions.setThrowExceptionOnScriptError(false)
    client.getOptions.setThrowExceptionOnFailingStatusCode(false)

      urls.flatMap(url =>
        try {
          val pageq = client.getPage(url).asInstanceOf[HtmlPage]

          val league = pageq.getByXPath[HtmlElement]("//*[@id=\"left-content\"]/div/div[2]/div[2]/div/h1").get(0).asText()
          val sameDateGames = pageq.getByXPath[HtmlElement]("//*[@id=\"left-content\"]/div/div[2]/div[2]/div/div[2]/div[3]/div")
          sameDateGames.asScala.flatMap(f = sameDateGroup => {
            val date = sameDateGroup.getByXPath[HtmlSpan]("div/div/span[2]").get(0).asText()
            val date1 = dateFormatNoYear.parse(date)
            val calendar = Calendar.getInstance()

            val now = calendar.getTime
            val currentYear = calendar.get(Calendar.YEAR)
            calendar.setTime(date1)
            calendar.set(Calendar.YEAR, currentYear)
            if (calendar.before(now)) {
              calendar.set(Calendar.YEAR, currentYear + 1)
            }

            val tbodys = sameDateGroup.getByXPath[HtmlTableBody]("div/table/tbody").asScala.toList

            val events = tbodys.filter(tbody => !getTableValue(tbody, 1, 2).contains("(+") && !getTableValue(tbody, 2, 2).contains("(+")) map (tbody => {

              val time = timeFormat.parse(getTableValue(tbody, 1, 1))
              val calendar1 = Calendar.getInstance()
              calendar1.setTimeZone(TimeZone.getTimeZone("Etc/GMT+8"))
              calendar1.setTime(time)
              calendar.set(Calendar.HOUR, calendar1.get(Calendar.HOUR))
              calendar.set(Calendar.MINUTE, calendar1.get(Calendar.MINUTE))

              try {
                val drawOdds = if (tbody.getChildElementCount > 2 && getTableValue(tbody, 3, 2) == "Draw") {
                  Some(BigDecimal(getTableValue(tbody, 3, 3)))
                }
                else None

                Some(Event(calendar.getTime, "", parseLeague(league), getTableValue(tbody, 1, 2), getTableValue(tbody, 2, 2), BigDecimal(getTableValue(tbody, 1, 3))
                  , BigDecimal(getTableValue(tbody, 2, 3)), drawOdds, getEventSourceName, ""))

              }
              catch {
                case t: Throwable => getLogger.error(s"error parsing ${tbody.asXml()}", t)
                  None
              }
            })

            events.flatten
          })
        }
        catch {
          case t: Throwable => getLogger.error(s"error parsing $url" ,t)
            List()
        })

  }

  private def getTableValue(tbody: HtmlTableBody, row: Int, column: Int): String = {
    tbody.getByXPath[HtmlSpan](s"tr[$row]/td[$column]/span").get(0).asText()
  }

  override def getEventSourceName: String = "pinnacle"
}