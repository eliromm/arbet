package model

import java.util.Date

/**
  * Created by E on 10/09/2016.
  */
case class Event(date: Date, id:Int, league:String, participant1:String, participant2:String,
                 moneyLine1:BigDecimal, moneyLine2:BigDecimal, draw:Option[BigDecimal]=None, provider:String, sourceXml:String)
