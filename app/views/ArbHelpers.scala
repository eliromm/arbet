package views

import bl.Arbitrage
import model.Event
import play.twirl.api.Html

import scala.math.BigDecimal.RoundingMode
import scala.xml.Elem

/**
  * Created by E on 01/12/2016.
  */
object ArbHelpers {

  def displayData(divId:String,name:String,events:Seq[Event]): Elem = {
    val arbitrageAnalysis = Arbitrage.compute(events)

    <div>
    <li>{name} margin is {arbitrageAnalysis.margin.setScale(4,RoundingMode.HALF_UP)}</li>

      {if ({arbitrageAnalysis.margin<1}){
      <div>
        bet1 is {arbitrageAnalysis.bet1.setScale(2,RoundingMode.HALF_UP)} at stake {arbitrageAnalysis.line1.setScale(2,RoundingMode.HALF_UP)}<br/>
        bet2 is {arbitrageAnalysis.bet2.setScale(2,RoundingMode.HALF_UP)} at stake {arbitrageAnalysis.line2.setScale(2,RoundingMode.HALF_UP)}<br/>
        bet3 is {arbitrageAnalysis.bet3.setScale(2,RoundingMode.HALF_UP)} at stake {arbitrageAnalysis.line3.setScale(2,RoundingMode.HALF_UP)}<br/>
        win is {arbitrageAnalysis.win.setScale(2,RoundingMode.HALF_UP)} %
      </div>
      }}


      <table  class="table table-striped" style ="border:1px solid #000;">
        {
        var i=0
        events.map(game => {
          i +=1

        val providerDivId = divId + "-" + i
        val tt = "showHideDiv('" + providerDivId + "')"
        <tr>

          <td>{game.date}</td><td>{game.id}</td><td>{game.league}</td>
          <td>{game.participant1}</td><td>{game.participant2}</td>
          <td>{ boldify(arbitrageAnalysis.line1,game.moneyLine1,events)}</td>
          <td>{ boldify(arbitrageAnalysis.line2,game.moneyLine2,events)}</td>
          <td>{ game.draw.map(res => boldify(arbitrageAnalysis.line3,res,events) ).getOrElse("")}</td>
          <td>{game.provider}</td>
          <td>

            <button onclick={tt}>source</button>
            <div id={providerDivId} style="display:none">{Html(game.sourceXml)}</div>
          </td>

        </tr>
      })}
      </table>
    </div>
  }

  def boldify(maxLine: BigDecimal,actualLine :BigDecimal,events :Seq[_]): Serializable ={
    val rounded = actualLine.setScale(2, RoundingMode.HALF_UP)

     if(maxLine==actualLine && events.size > 1) <b>{rounded}</b> else rounded

  }
}