package services

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.betfair.Configuration
import com.betfair.domain._
import com.betfair.service.{BetfairServiceNG, BetfairServiceNGCommand, BetfairServiceNGException}
import com.typesafe.config.ConfigFactory
import model.Event

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by E on 09/12/2016.
  */
object BetFairService extends EventService {

  implicit val system = ActorSystem()
  import system.dispatcher
  val conf = ConfigFactory.load()
  val appKey = conf.getString("betfairService.appKey")
  val username = conf.getString("betfairService.username")
  val password = conf.getString("betfairService.password")
  val apiUrl = conf.getString("betfairService.apiUrl")
  val isoUrl = conf.getString("betfairService.isoUrl")

  val config = Configuration(appKey, username, password, apiUrl, isoUrl)

  val command = new BetfairServiceNGCommand(config)


  val competitions=Seq(59,61,55,81,83,31,7129730,117,9404054,11349530)


  override def getEvents(live: Boolean): Seq[Event] = {

    val betfairServiceNG = new BetfairServiceNG(config, command)

    // log in to obtain a session id
    val sessionTokenFuture = betfairServiceNG.login map {
      case Some(loginResponse) =>
        getLogger.info(s"result is $loginResponse")
        loginResponse.token
      case x => throw BetfairServiceNGException(s"no session token, response was $x")
    }

    val sessionToken = Await.result(sessionTokenFuture, 10 seconds)


    val result = competitions.map(competitionId => {

      val marketIds = betfairServiceNG.listMarketCatalogue(sessionToken, new MarketFilter(marketTypeCodes = Set("MATCH_ODDS"), competitionIds = Set(competitionId))
        , List(MarketProjection.MARKET_START_TIME,
          MarketProjection.RUNNER_DESCRIPTION,
          MarketProjection.EVENT_TYPE,
          MarketProjection.EVENT,
          MarketProjection.COMPETITION), MarketSort.FIRST_TO_START, 500
      ) map {
        case Some(listMarketCatalogueContainer) =>
          getLogger.info(s"catalog is $listMarketCatalogueContainer")
          listMarketCatalogueContainer.result
        case None =>
          List()
      }

      // list market book with Exchange Best Offers
      val priceProjection = PriceProjection(priceData = Set(PriceData.EX_BEST_OFFERS))

      val events = marketIds.flatMap(resultList => {
        val toSet = resultList.map(_.marketId).toSet
        if (toSet.isEmpty){
          Future.successful(List())
        }
        else {

          val book = betfairServiceNG.listMarketBook(sessionToken, marketIds = toSet, Some(("priceProjection", priceProjection)))
          book.map {
            case Some(listMarketBookContainer) =>
              listMarketBookContainer.result.flatMap(marketBook => {
                convertMarketBooktoEvent(marketBook, resultList.find(_.marketId == marketBook.marketId).get)
              })
            case None =>
              List()
          }
        }
      })
      events
    })

    Await.result(Future.sequence(result),Duration(10,TimeUnit.SECONDS)).flatten
  }

  def convertMarketBooktoEvent(marketBook: MarketBook,marketCatalogue: MarketCatalogue) :Option[Event] = {

    try {
      val runnerList = marketCatalogue.runners.get
      val runner1 = runnerList(0)
      val runner2 = runnerList(1)
      val runner3 = if(runnerList.size==3) Some(runnerList(2)) else None
      Some(Event(marketCatalogue.marketStartTime.get.toDate,"",
        parseLeague(marketCatalogue.competition.get.name),runner1.runnerName,runner2.runnerName,
        BigDecimal(marketBook.runners.find(_.selectionId == runner1.selectionId).get.ex.get.availableToBack.head.price),
        BigDecimal(marketBook.runners.find(_.selectionId == runner2.selectionId).get.ex.get.availableToBack.head.price),
        runner3.map(res => BigDecimal(marketBook.runners.find(_.selectionId == res.selectionId).get.ex.get.availableToBack.head.price)),
        getEventSourceName,marketBook.toString + marketCatalogue.toString
      ) )
    }
    catch {
      case e:Exception =>
        getLogger.error(s"couldnt parse event from marketbook=$marketBook and marketCatalouge=$marketCatalogue",e)
        None
    }
  }

  override def getEventSourceName: String = "betfair"
}
