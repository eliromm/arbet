package controllers

import bl.EventManager
import model.League
import play.api.mvc.{Action, Controller}

object Application extends Controller {
  def index = Action {
    Ok(views.html.index(""))
  }
  def results(league:Option[String]) = Action {

    val leagueName = league.flatMap(name => {
      League.Leagues.find(_.toString.compareToIgnoreCase(name)==0)
    })

    val stringToList = leagueName.fold(EventManager.storage)(lea => EventManager.storage.filter(_._2.exists(_.league == lea.toString)))

    Ok(views.html.results(stringToList.toList.sortBy(_._2.head.date)))
  }

  def resultsdate = Action {
    Ok(views.html.results(EventManager.storage.toList.sortBy(_._2.head.date)))
  }

  def load(live:Option[Boolean]) = Action {
    val events = EventManager.loadEvents(live.getOrElse(false))
    val merged = EventManager.mergeEvents()
    val arbs = EventManager.computeArbs()

    Ok(views.html.loaded(events.toList))
  }
  def mergeEvents = Action {
    val merged = EventManager.mergeEvents()
    val arbs = EventManager.computeArbs()

    Ok(views.html.merged(merged))
  }

  def arbs = Action {
    val arbs = EventManager.computeArbs()
    Ok(views.html.results(arbs.toList))
  }
}