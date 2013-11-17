package models

import play.api.db.DB
import play.api.Play.current
import org.joda.time.DateTime

case class CreateAddress(
  countryCode: CountryCode,
  firstName: String,
  middleName: String,
  lastName: String,
  firstNameKana: String,
  lastNameKana: String,
  zip1: String,
  zip2: String,
  zip3: String,
  prefecture: Prefecture,
  address1: String,
  address2: String,
  address3: String,
  address4: String,
  address5: String,
  tel1: String,
  tel2: String,
  tel3: String,
  shippingDate: DateTime,
  comment: String
) extends NotNull {
  lazy val hasName: Boolean = !firstName.isEmpty || !lastName.isEmpty
  lazy val hasKanaName: Boolean = !firstNameKana.isEmpty || !lastNameKana.isEmpty
  lazy val hasZip: Boolean = !zip1.isEmpty || !zip2.isEmpty || !zip3.isEmpty

  def save(userId: Long): Address = {
    DB.withTransaction { implicit conn =>
      val addr = Address(
        countryCode = this.countryCode,
        firstName = this.firstName,
        middleName = this.middleName,
        lastName = this.lastName,
        firstNameKana = this.firstNameKana,
        lastNameKana = this.lastNameKana,
        zip1 = this.zip1,
        zip2 = this.zip2,
        zip3 = this.zip3,
        prefecture = this.prefecture,
        address1 = this.address1,
        address2 = this.address2,
        address3 = this.address3,
        address4 = this.address4,
        address5 = this.address5,
        tel1 = this.tel1,
        tel2 = this.tel2,
        tel3 = this.tel3,
        comment = this.comment
      )

      ShoppingCartItem.sites(userId).foreach { siteId =>
        ShoppingCartShipping.updateOrInsert(userId, siteId, shippingDate.getMillis)
      }

      ShippingAddressHistory.createNew(userId, addr)
      addr
    }
  }
}

object CreateAddress {
  def fromAddress(addr: Address, shippingDate: DateTime) =
    CreateAddress(
      addr.countryCode,
      addr.firstName,
      addr.middleName,
      addr.lastName,
      addr.firstNameKana,
      addr.lastNameKana,
      addr.zip1,
      addr.zip2,
      addr.zip3,
      addr.prefecture,
      addr.address1,
      addr.address2,
      addr.address3,
      addr.address4,
      addr.address5,
      addr.tel1,
      addr.tel2,
      addr.tel3,
      shippingDate,
      addr.comment
    )

  def apply4Japan(
    firstName: String,
    lastName: String,
    firstNameKana: String,
    lastNameKana: String,
    zip1: String,
    zip2: String,
    prefecture: Int,
    address1: String,
    address2: String,
    address3: String,
    address4: String,
    address5: String,
    tel1: String,
    tel2: String,
    tel3: String,
    shippingDate: DateTime,
    comment: String
  ) = CreateAddress(
    CountryCode.JPN,
    firstName,
    "",
    lastName,
    firstNameKana,
    lastNameKana,
    zip1,
    zip2,
    "",
    JapanPrefecture.byIndex(prefecture),
    address1,
    address2,
    address3,
    address4,
    address5,
    tel1,
    tel2,
    tel3,
    shippingDate,
    comment
  )

  def unapply4Japan(addr: CreateAddress): Option[(
    String, String,
    String, String,
    String, String,
    Int,
    String, String, String, String, String,
    String, String, String, DateTime, String
  )] = Some((addr.firstName, addr.lastName,
             addr.firstNameKana, addr.lastNameKana,
             addr.zip1, addr.zip2,
             addr.prefecture.code,
             addr.address1, addr.address2, addr.address3, addr.address4, addr.address5,
             addr.tel1, addr.tel2, addr.tel3, addr.shippingDate, addr.comment
           ))
}
