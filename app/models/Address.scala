package models

import anorm._
import anorm.{NotAssigned, Pk}
import anorm.SqlParser
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import play.api.i18n.Lang
import java.sql.Connection
import helpers.Enums

case class Address(
  id: Pk[Long] = NotAssigned,
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
  comment: String
) {
  lazy val hasName: Boolean = !firstName.isEmpty || !lastName.isEmpty
  lazy val hasKanaName: Boolean = !firstNameKana.isEmpty || !lastNameKana.isEmpty
  lazy val hasZip: Boolean = !zip1.isEmpty || !zip2.isEmpty || !zip3.isEmpty
}

case class ShippingAddressHistory(
  id: Pk[Long] = NotAssigned,
  storeUserId: Long,
  addressId: Long,
  updatedTime: Long
)

object Address {
  val simple = {
    SqlParser.get[Pk[Long]]("address.address_id") ~
    SqlParser.get[Int]("address.country_code") ~
    SqlParser.get[String]("address.first_name") ~
    SqlParser.get[String]("address.middle_name") ~
    SqlParser.get[String]("address.last_name") ~
    SqlParser.get[String]("address.first_name_kana") ~
    SqlParser.get[String]("address.last_name_kana") ~
    SqlParser.get[String]("address.zip1") ~
    SqlParser.get[String]("address.zip2") ~
    SqlParser.get[String]("address.zip3") ~
    SqlParser.get[Int]("address.prefecture") ~
    SqlParser.get[String]("address.address1") ~
    SqlParser.get[String]("address.address2") ~
    SqlParser.get[String]("address.address3") ~
    SqlParser.get[String]("address.address4") ~
    SqlParser.get[String]("address.address5") ~
    SqlParser.get[String]("address.tel1") ~
    SqlParser.get[String]("address.tel2") ~
    SqlParser.get[String]("address.tel3") ~
    SqlParser.get[String]("address.comment") map {
      case id~countryCode~firstName~middleName~lastName~firstNameKana~lastNameKana~zip1~zip2~zip3~prefecture~
        address1~address2~address3~address4~address5~tel1~tel2~tel3~comment => {
          val cc = CountryCode.byIndex(countryCode)
          val pref = 
            if (cc == CountryCode.JPN) JapanPrefecture.byIndex(prefecture) else UnknownPrefecture.UNKNOWN

          Address(id, cc,
                  firstName, middleName, lastName,
                  firstNameKana, lastNameKana,
                  zip1, zip2, zip3,
                  pref,
                  address1, address2, address3, address4, address5,
                  tel1, tel2, tel3, comment)
        }
    }
  }

  def createNew(
    countryCode: CountryCode,
    firstName: String = "",
    middleName: String = "",
    lastName: String = "",
    firstNameKana: String = "",
    lastNameKana: String = "",
    zip1: String = "",
    zip2: String = "",
    zip3: String = "",
    prefecture: Prefecture = UnknownPrefecture.UNKNOWN,
    address1: String = "",
    address2: String = "",
    address3: String = "",
    address4: String = "",
    address5: String = "",
    tel1: String = "",
    tel2: String = "",
    tel3: String = "",
    comment: String = ""
  )(implicit conn: Connection): Address = {
    SQL(
      """
      insert into address (
        address_id, country_code,
        first_name, middle_name, last_name,
        first_name_kana, last_name_kana,
        zip1, zip2, zip3,
        prefecture,
        address1, address2, address3, address4, address5,
        tel1, tel2, tel3, comment
      ) values (
        (select nextval('address_seq')),
        {countryCode},
        {firstName}, {middleName}, {lastName},
        {firstNameKana}, {lastNameKana},
        {zip1}, {zip2}, {zip3},
        {prefecture},
        {address1}, {address2}, {address3}, {address4}, {address5},
        {tel1}, {tel2}, {tel3}, {comment}
      )
      """
    ).on(
      'countryCode -> countryCode.ordinal,
      'firstName -> firstName,
      'middleName -> middleName,
      'lastName -> lastName,
      'firstNameKana -> firstNameKana,
      'lastNameKana -> lastNameKana,
      'zip1 -> zip1,
      'zip2 -> zip2,
      'zip3 -> zip3,
      'prefecture -> prefecture.code,
      'address1 -> address1,
      'address2 -> address2,
      'address3 -> address3,
      'address4 -> address4,
      'address5 -> address5,
      'tel1 -> tel1,
      'tel2 -> tel2,
      'tel3 -> tel3,
      'comment -> comment
    ).executeUpdate()

    val id = SQL("select currval('address_seq')").as(SqlParser.scalar[Long].single)

    Address(Id(id), countryCode,
            firstName, middleName, lastName,
            firstNameKana, lastNameKana,
            zip1, zip2, zip3,
            prefecture,
            address1, address2, address3, address4, address5,
            tel1, tel2, tel3, comment)
  }

  def byId(id: Long)(implicit conn: Connection): Address =
    SQL(
      """
      select * from address where address_id = {id}
      """
    ).on('id -> id).as(simple.single)

  lazy val JapanPrefectures = Enums.toDropdownTable(JapanPrefecture.all)
}

object ShippingAddressHistory {
  val HistoryMaxCount = 3

  val simple = {
    SqlParser.get[Pk[Long]]("shipping_address_history.shipping_address_history_id") ~
    SqlParser.get[Long]("shipping_address_history.store_user_id") ~
    SqlParser.get[Long]("shipping_address_history.address_id") ~
    SqlParser.get[java.util.Date]("shipping_address_history.updated_time") map {
      case id~userId~addressId~updatedTime =>
        ShippingAddressHistory(id, userId, addressId, updatedTime.getTime)
    }
  }

  def createNew(
    userId: Long, address: Address, now: Long = System.currentTimeMillis
  )(implicit conn: Connection) {
    val updateCount = SQL(
      """
      update shipping_address_history
      set updated_time = {now}
      where shipping_address_history_id = (
        select shipping_address_history_id from shipping_address_history
        inner join address on shipping_address_history.address_id = address.address_id
        where shipping_address_history.store_user_id = {userId}
        and country_code = {countryCode}
        and first_name = {firstName}
        and middle_name = {middleName}
        and last_name = {lastName}
        and first_name_kana = {firstNameKana}
        and last_name_kana = {lastNameKana}
        and zip1 = {zip1}
        and zip2 = {zip2}
        and zip3 = {zip3}
        and prefecture = {prefecture}
        and address1 = {address1}
        and address2 = {address2}
        and address3 = {address3}
        and address4 = {address4}
        and address5 = {address5}
        and tel1 = {tel1}
        and tel2 = {tel2}
        and tel3 = {tel3}
        and comment = {comment}
      )
      """
    ).on(
      'now -> new java.sql.Date(now),
      'userId -> userId,
      'countryCode -> address.countryCode.code,
      'firstName -> address.firstName,
      'middleName -> address.middleName,
      'lastName -> address.lastName,
      'firstNameKana -> address.firstNameKana,
      'lastNameKana -> address.lastNameKana,
      'zip1 -> address.zip1,
      'zip2 -> address.zip2,
      'zip3 -> address.zip3,
      'prefecture -> address.prefecture.code,
      'address1 -> address.address1,
      'address2 -> address.address2,
      'address3 -> address.address3,
      'address4 -> address.address4,
      'address5 -> address.address5,
      'tel1 -> address.tel1,
      'tel2 -> address.tel2,
      'tel3 -> address.tel3,
      'comment -> address.comment
    ).executeUpdate()
      
    if (updateCount == 0) {
      val newAddress = Address.createNew(
        address.countryCode,
        address.firstName,
        address.middleName,
        address.lastName,
        address.firstNameKana,
        address.lastNameKana,
        address.zip1,
        address.zip2,
        address.zip3,
        address.prefecture,
        address.address1,
        address.address2,
        address.address3,
        address.address4,
        address.address5,
        address.tel1,
        address.tel2,
        address.tel3,
        address.comment
      )

      SQL(
        """
        insert into shipping_address_history (
          shipping_address_history_id, store_user_id, address_id, updated_time
        ) values (
          (select nextval('shipping_address_history_seq')),
          {userId}, {addressId}, {now}
        )
        """
      ).on(
        'userId -> userId,
        'addressId -> newAddress.id.get,
        'now -> new java.sql.Date(now)
      ).executeUpdate()

      SQL(
        """
        delete from shipping_address_history
        where shipping_address_history_id in (
          select shipping_address_history_id from shipping_address_history
          where store_user_id = {userId}
          order by updated_time desc
          limit null offset {offset}
        )
        """
      ).on(
        'userId -> userId,
        'offset -> HistoryMaxCount
      ).executeUpdate()
    }
  }

  def list(userId: Long)(implicit conn: Connection): Seq[ShippingAddressHistory] =
    SQL(
      """
      select * from shipping_address_history
      where store_user_id = {userId}
      order by updated_time desc
      """
    ).on(
      'userId -> userId
    ).as(
      simple *
    )
}
