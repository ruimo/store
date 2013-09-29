package models

import org.specs2.mutable._

class ShippingCalculatorSpec extends Specification {
  "ShippingCalculator" should {
    "Earn empty map when no items." in {
      ShippingFeeEntries().bySiteAndItemClass.size === 0
    }

    "On item earns single entry." in {
      val e = ShippingFeeEntries().add(1L, 2, 5)

      e.bySiteAndItemClass.size === 1
      val byItemClass = e.bySiteAndItemClass(1L)
      byItemClass.size === 1
      byItemClass(2) === 5
    }

    "Quantity should added." in {
      val e = ShippingFeeEntries()
        .add(1L, 2, 5)
        .add(1L, 2, 10)

      e.bySiteAndItemClass.size === 1
      val byItemClass = e.bySiteAndItemClass(1L)
      byItemClass.size === 1
      byItemClass(2) === 15
    }

    "Two item classes earn two entries." in {
      val e = new ShippingFeeEntries()
        .add(1L, 2, 5)
        .add(1L, 2, 10)
        .add(1L, 3, 10)

      e.bySiteAndItemClass.size === 1
      val byItemClass = e.bySiteAndItemClass(1L)
      byItemClass.size === 2
      byItemClass(2) === 15
      byItemClass(3) === 10
    }

    "Two sites and two items classes earn four entries." in {
      val e = ShippingFeeEntries()
        .add(1L, 2, 5)
        .add(1L, 2, 10)
        .add(1L, 3, 10)
        .add(2L, 2, 3)
        .add(2L, 3, 2)

      e.bySiteAndItemClass.size === 2
      val byItemClass1 = e.bySiteAndItemClass(1L)
      byItemClass1.size === 2
      byItemClass1(2) === 15
      byItemClass1(3) === 10

      val byItemClass2 = e.bySiteAndItemClass(2L)
      byItemClass2.size === 2
      byItemClass2(2) === 3
      byItemClass2(3) === 2
    }
  }
}
