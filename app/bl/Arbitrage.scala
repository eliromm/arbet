package bl

import model.{ArbitrageAnalysis, Event}
import play.api.Logger

/**
  * Created by E on 10/09/2016.
  */
object Arbitrage {

  def compute(events: Seq[Event]): ArbitrageAnalysis = {
    val maxResult1 = events.map(_.moneyLine1).max
    val maxResult2 = events.map(_.moneyLine2).max
    val maybeMaxResult3 = events.map(_.draw).max
    val event = events.head

    if (maybeMaxResult3.isDefined) {
      val maxResult3 = maybeMaxResult3.get
      val margin = 1 / maxResult1 + 1 / maxResult2 + 1 / maxResult3
      if (margin < 1) {
        val stake1 = 100 / (1 + maxResult1 / maxResult2 + maxResult1 / maxResult3)
        val stake2 = 100 / (1 + maxResult2 / maxResult1 + maxResult2 / maxResult3)
        val stake3 = 100 - stake1 - stake2

        val res = stake1 * maxResult1
        val win = res - 100

        Logger.debug(s"-----------------------\n" +
          s"margin = $margin\n" +
          s"event id is ${event.id}\n" +
          s"teams are ${event.participant1} vs ${event.participant2} \n" +
          s"events is $events\n" +
          s"bet1 is $maxResult1\n" +
          s"bet2 is $maxResult2\n" +
          s"bet3 is $maxResult3\n" +
          s"stake1 is $stake1\n" +
          s"stake2 is $stake2\n" +
          s"stake3 is $stake3\n" +
          s"win is $win%\n" +
          s"-----------------------------------------")

        ArbitrageAnalysis(margin, maxResult1, maxResult2, maxResult3, stake1, stake2, stake3, win)
      }
      else ArbitrageAnalysis(margin, 0, 0, 0, 0, 0, 0, 0)

    }

    else {

      val margin = 1 / maxResult1 + 1 / maxResult2
      if (margin < 1) {
        val stake1 = 100 / (1 + maxResult1 / maxResult2)
        val stake2 = 100 - stake1

        val res = stake1 * maxResult1
        val win = res - 100
        Logger.debug(s"-----------------------\n" +
          s"margin = $margin\n" +
          s"event id is ${event.id}\n" +
          s"teams are ${event.participant1} vs ${event.participant2} \n" +
          s"events is $events\n" +
          s"bet1 is $maxResult1\n" +
          s"bet2 is $maxResult2\n" +
          s"stake1 is $stake1\n" +
          s"stake2 is $stake2\n" +
          s"win is $win%\n" +
          s"-----------------------------------------")

        ArbitrageAnalysis(margin, maxResult1, maxResult2, 0, stake1, stake2, 0, win)
      }
      else ArbitrageAnalysis(margin, 0, 0, 0, 0, 0, 0, 0)
    }
  }
}
