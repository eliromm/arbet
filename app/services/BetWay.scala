package services

import java.text.SimpleDateFormat

import model.Event

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


/**
  * Created by e on 18/06/17.
  */
object BetWay extends EventService {

  val dateFormat = new SimpleDateFormat("MM/dd/yyy HH:mm")

  override def getEvents(live: Boolean): Seq[Event] = {

    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    import play.api.libs.ws.ning.NingWSClient
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val betwayEventReads: Reads[BetwayEvent] = (
      (__ \ "Date").read[String] and
        (__ \ "Time").read[String] and
        (__ \ "GroupName").read[String] and
        (__ \ "Markets").read[List[Int]]
      ) (BetwayEvent.apply _)

    implicit val marketReads: Reads[Market] = (
      (__ \ "Id").read[Int] and
        (__ \ "Title").read[String] and
        (__ \ "Headers").read[List[String]] and
        (__ \ "Outcomes").read[List[List[Int]]]
      ) (Market.apply _)

    implicit val outcomeReads: Reads[Outcome] = (
      (__ \ "OddsDecimal").read[BigDecimal] and
        (__ \ "Id").read[Int]
      ) (Outcome.apply _)


    implicit val betwayReads: Reads[BetwayResponse] = (
      (__ \ "Events").read[List[BetwayEvent]] and
        (__ \ "Markets").read[List[Market]] and
        (__ \ "Outcomes").read[List[Outcome]]
      ) (BetwayResponse.apply _)


    val client = NingWSClient()

    val leagues = List(("england", "premier-league"), ("germany", "bundesliga")
      ,("european-cups","uefa-champions-league"),("european-cups","uefa-europa-league"))

    val eventualEventIds = leagues.map(leagueName => {
      val eventsRequest = client.url("https://sports.betway.com/api/Events/V2/GetGroup?t=445eb573-34fc-4072-8dcc-9958c3c9a80c").
        withHeaders(("Content-Type", "application/json"))


      val country = "england"
      val league = "premier-league"
      val eventualEventsResponse = eventsRequest.post("{\"PremiumOnly\":false,\"LanguageId\":1,\"ClientTypeId\":2,\"BrandId\":3,\"JurisdictionId\":2,\"ClientIntegratorId\":1,\"CategoryCName\":\"soccer\",\"SubCategoryCName\":\"%s\",\"GroupCName\":\"%s\",\"ApplicationId\":5,\"BrowserId\":3,\"OsId\":5,\"ApplicationVersion\":\"\",\"BrowserVersion\":\"58.0.3029.110\",\"OsVersion\":\"\",\"SessionId\":null,\"TerritoryId\":82}".format(leagueName._1, leagueName._2))

      eventualEventsResponse.map(response => {
        (response.json \ "Categories").get.asInstanceOf[JsArray].value.flatMap(a => (a \ "Events").get.as[List[JsValue]])

      })
    })

    val eventualEventIdsFlat = Future.sequence(eventualEventIds).map(_.flatten)


    val res = eventualEventIdsFlat.flatMap(eventIds => {

      val request = client.url("https://sports.betway.com/api/Events/V2/GetEvents?t=34129a6b-fba8-4cbe-8d2e-c4ed68a72ef8").
        withHeaders(("Content-Type", "application/json"))

      val eventualResponse = request.post("{\"LanguageId\":1,\"ClientTypeId\":2,\"BrandId\":3,\"JurisdictionId\":2,\"ClientIntegratorId\":1,\n    \"ExternalIds\":" + JsArray(eventIds).toString() + ",\n    \"MarketCName\":\"win-draw-win\",\"ScoreboardRequest\":{\"ScoreboardType\":3,\"IncidentRequest\":{}},\"ApplicationId\":5,\"BrowserId\":3,\"OsId\":5,\n    \"ApplicationVersion\":\"\",\"BrowserVersion\":\"58.0.3029.110\",\"OsVersion\":\"\",\"SessionId\":null,\"TerritoryId\":82}")


      eventualResponse.map(response => {
        val betwayResponse = response.json.as[BetwayResponse]

        val markets = betwayResponse.markets.map(market => (market.id, market)).toMap
        val outcomes = betwayResponse.outcome.map(outcome => (outcome.id, outcome.odds)).toMap
        betwayResponse.events.flatMap(event => {

          event.markets.map(foundMarket => {
            val market = markets(foundMarket)
            Event(dateFormat.parse(event.date + " " + event.time), market.id.toString, parseLeague(event.league),
              market.teams(0), market.teams(2), outcomes(market.outcomeIds(0)(0)),
              outcomes(market.outcomeIds(0)(2)), Some(outcomes(market.outcomeIds(0)(1))),
              getEventSourceName, response.body)

          })

        })
      })

    })

    Await.result(res, Duration.apply("10 sec"))
  }

  override def getEventSourceName: String = "betway"
}


case class BetwayResponse(events: List[BetwayEvent], markets: List[Market], outcome: List[Outcome])

case class BetwayEvent(date: String, time: String, league: String, markets: List[Int])

case class Market(id: Int, title: String, teams: List[String], outcomeIds: List[List[Int]])

case class Outcome(odds: BigDecimal, id: Int)