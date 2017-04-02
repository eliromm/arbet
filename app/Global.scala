/**
  * Created by E on 30/11/2016.
  */

import bl.EventManager
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Application has started")
    EventManager.deserializeData
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

}