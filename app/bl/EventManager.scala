package bl

import java.io._
import java.text.{Normalizer, SimpleDateFormat}
import java.util.Date

import model.Event
import org.apache.commons.lang3.{StringEscapeUtils, StringUtils}
import play.api.Play.current
import play.api.{Logger, Play}
import services._

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.parallel.ParSeq
import scala.util.Try
import collection.JavaConversions._

/**
  * Created by E on 18/09/2016.
  */
object EventManager {
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val dataPath = Play.configuration.getString("online_data.path").getOrElse("online_data") + "/"
  val teamsNames: List[List[String]] = Play.configuration.getList("tuples").map(_.toList.map(_.unwrapped().asInstanceOf[java.util.ArrayList[String]].toList)).getOrElse(List())
  val availableExchanges = Map(("betfred", BetfredSource), ("pinnacle", PinnacleSource), ("intertops", IntertopsSource),
    ("bookmakerseu", BookmakersEuSource), ("betclic", BetclicSource), ("betfair", BetFairService), ("smarkets", SmarketsSource), ("betway", BetWay))

  var storage = Map[String, List[Event]]()
  var arbs = Map[String, List[Event]]()
  var events = List[Event]()

  def deserializeData(): Unit = {
    try {
      val is = new ObjectInputStream(new FileInputStream(dataPath + "storage.dat")) {
        override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
          try {
            Class.forName(desc.getName, false, getClass.getClassLoader)
          }
          catch {
            case ex: ClassNotFoundException => super.resolveClass(desc)
          }
        }

      }
      val obj = is.readObject()
      is.close()
      storage = obj.asInstanceOf[HashMap[String, List[Event]]]
    } catch {
      case t: Throwable => Logger.warn("problem deserializing storage data", t)
    }
    try {
      val is = new ObjectInputStream(new FileInputStream(dataPath + "events.dat")) {
        override def resolveClass(desc: java.io.ObjectStreamClass): Class[_] = {
          try {
            Class.forName(desc.getName, false, getClass.getClassLoader)
          }
          catch {
            case ex: ClassNotFoundException => super.resolveClass(desc)
          }
        }

      }
      val obj = is.readObject()
      is.close()
      events = obj.asInstanceOf[List[Event]]
    } catch {
      case t: Throwable => Logger.warn("problem deserializing events data", t)
    }
  }

  def computeArbs(): Map[String, List[Event]] = {
    arbs = storage.filter(a => Arbitrage.compute(a._2).margin < 1)
    arbs
  }

  def loadEvents(live: Boolean): ParSeq[(EventService, Seq[Event])] = {
    val seq = Play.configuration.getStringSeq("exchanges").getOrElse(Seq())
    val services = seq.flatMap(exchange => availableExchanges.get(exchange))
    Logger.info(s"services are ${services.map(_.getEventSourceName)}")
    val serviceEvents = services.par.map(service => {

      val events = Try(service.getEvents(live)).recover { case e: Throwable =>
        Logger.error(s"exception when getting events for ${service.getEventSourceName}", e)
        Seq()
      }
      (service, events.get)
    })

    this.events = serviceEvents.flatten(_._2).toList

    serviceEvents
  }

  def mergeEvents(): (Int, Int) = {
    val storage = new mutable.HashMap[String, List[Event]]

    events.foreach(event => {
      val key = createKey(event.participant1, event.participant2, event.date)
      val key2 = createKey(event.participant2, event.participant1, event.date)
      val res2 = storage.get(key2)
      if (res2.isDefined) storage.put(key2, res2.get.
        +:(event.copy(participant1 = event.participant2, participant2 = event.participant1, moneyLine1 = event.moneyLine2, moneyLine2 = event.moneyLine1)))
      else {
        val res = storage.getOrElseUpdate(key, List())
        storage.put(key, res.+:(event))
      }
    }
    )
    this.storage = storage.toMap

    serializeData()

    (events.size, storage.size)

  }

  def serializeData(): Unit = {
    val file = new File(dataPath + "storage.dat")
    file.getParentFile.mkdirs()
    val out = new FileOutputStream(file)
    val oss = new ObjectOutputStream(out)
    oss.writeObject(storage)
    oss.close()
    val ose = new ObjectOutputStream(new FileOutputStream(dataPath + "events.dat"))
    ose.writeObject(events)
    ose.close()
  }

  def createKey(p1: String, p2: String, date: Date): String = {

    def nomalForm(s: String): String = {
      normalize(StringUtils.stripAccents(StringEscapeUtils.unescapeHtml4(Normalizer.normalize(s.trim, Normalizer.Form.NFKC)).toLowerCase).replaceAll(" +"," "))
    }

    s"${nomalForm(p1)} vs ${nomalForm(p2)}@${dateFormat.format(date)}"
  }

  def normalize(name: String): String = {
    teamsNames.find(_.contains(name)).map(_.head).getOrElse(name)
  }
}