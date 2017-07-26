package model

/**
  * Created by E on 01/12/2016.
  */
object League {
  sealed trait EnumVal
  case object Italy1 extends EnumVal
  case object Germany1 extends EnumVal
  case object Germany2 extends EnumVal
  case object Spain1 extends EnumVal
  case object Dutch1 extends EnumVal
  case object WTA extends EnumVal
  case object ATP extends EnumVal
  case object CL extends EnumVal
  case object EUROPA extends EnumVal
  case class unknown(name:String) extends EnumVal
  val Leagues  = Seq(Italy1,Dutch1, Germany1, Germany2, Spain1, WTA, ATP,CL,EUROPA)

  def apply(name: String): League.EnumVal = Leagues.find(_.toString == name).getOrElse(unknown(name))

}
