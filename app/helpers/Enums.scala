package helpers

object Enums {
  def toDropdownTable[E <: Enum[E]](clazz: Class[E]): Seq[(String, String)] = 
    clazz.getEnumConstants.map {
      e => e.ordinal.toString -> e.toString
    }

  def toDropdownTable[E <: Enum[E]](ary: Array[E]): Seq[(String, String)] =
    ary.map {
      e => e.ordinal.toString -> e.toString
    }

  def toTable[E <: Enum[E]](ary: Array[E]): Seq[(Int, String)] =
    ary.map {
      e => e.ordinal -> e.toString
    }
}
