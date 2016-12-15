package models

import java.nio.file.{Path, Files}
import java.io.{BufferedReader, StringReader}
import scala.collection.immutable
import scala.annotation.tailrec
import java.time.format.DateTimeFormatter
import java.time.Instant
import play.api.Play.current
import play.api.db.DB
import scala.util.control.TailCalls._
import scala.util.Try
import java.sql.Connection
import play.api.Logger

object ItemCsv {
  class FieldParser[T](to: String => T) {
    def parse(lineNo: Int, colNo: Int, name: String, s: String): Option[T] = s match {
      case "" => None
      case str => try {
        Some(to(str))
      }
      catch {
        case nfe: NumberFormatException =>
          throw new InvalidColumnException(lineNo, colNo, name + " is invalid", str, nfe)
      }
    }
  }
  case class EpochMilli(value: Long) extends AnyVal
  implicit object BigDecimalFieldParser extends FieldParser[BigDecimal](BigDecimal.apply)
  implicit object LongFieldParser extends FieldParser[Long](_.toLong)
  val YyyyMmDdHhMmSsParserFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  object YyyyMmDdHhMmSsParser extends FieldParser[EpochMilli](
    s => EpochMilli(java.sql.Timestamp.valueOf(s).getTime)
  )

  def to[T](lineNo: Int, colNo: Int, name: String, s: String)(implicit parser: FieldParser[T]): Option[T] =
    parser.parse(lineNo, colNo, name, s)
  def toMandatory[T](lineNo: Int, colNo: Int, name: String, s: String)(implicit parser: FieldParser[T]): T =
    parser.parse(lineNo, colNo, name, s).getOrElse {
      throw new InvalidColumnException(lineNo, colNo, name + " is empty", s)
    }

  case class ItemNumericMetadataField(
    s: String
  )

  case class ItemTextMetadataField(
    s: String
  )

  case class SiteItemNumericMetadataField(
    typeCode: SiteItemNumericMetadataType,
    value: Long,
    until: Long
  )

  object SiteItemNumericMetadataField {
    def apply(lineNo: Int, colNo: Int, s: String): SiteItemNumericMetadataField = {
      val split = s.split("/")
      SiteItemNumericMetadataField(
        try {
          SiteItemNumericMetadataType.byIndex(
            getLongRemovingAfterColonComment(split(0)).getOrElse {
              throw new InvalidColumnException(lineNo, colNo, "Invalid site item numeric metadata code", s)
            }.toInt
          )
        }
        catch {
          case t: ArrayIndexOutOfBoundsException =>
            throw new InvalidColumnException(lineNo, colNo, "Invalid site item numeric metadata code", s)
          case t: NumberFormatException =>
            throw new InvalidColumnException(lineNo, colNo, "Invalid site item numeric metadata code", s)
        },
        toMandatory[Long](lineNo, colNo, "site item numeric metadata value", split(1)),
        toMandatory[EpochMilli](lineNo, colNo, "site item numeric metadata until", split(2))(YyyyMmDdHhMmSsParser).value
      )
    }
  }

  case class SiteItemTextMetadataField(
    typeCode: SiteItemTextMetadataType,
    value: String
  )

  object SiteItemTextMetadataField {
    def apply(lineNo: Int, colNo: Int, s: String): SiteItemTextMetadataField = {
      val idx = s.indexOf("/")
      if (idx == -1) throw new InvalidColumnException(lineNo, colNo, "Invalid site item text metadata", s)

      val typeCode = s.substring(0, idx)
      SiteItemTextMetadataField(
        try {
          SiteItemTextMetadataType.byIndex(
            getLongRemovingAfterColonComment(typeCode).getOrElse {
              throw new InvalidColumnException(lineNo, colNo, "Invalid site item text metadata code", s)
            }.toInt
          )
        }
        catch {
          case t: ArrayIndexOutOfBoundsException =>
            throw new InvalidColumnException(lineNo, colNo, "Invalid site item text metadata code", s)
          case t: NumberFormatException =>
            throw new InvalidColumnException(lineNo, colNo, "Invalid site item text metadata code", s)
        },
        s.substring(idx + 1)
      )
    }
  }

  abstract class ItemCsvParseException(
    val lineNo: Int, msg: Option[String] = None, cause: Throwable = null
  ) extends Exception("line " + lineNo + msg.map(": " + _).getOrElse(""), cause)

  class InvalidColumnException(
    lineNo: Int, val colNo: Int, val desc: String, val value: String, cause: Throwable = null
  ) extends ItemCsvParseException(
    lineNo, Some("Invalid col(" + colNo + ") '" + desc + "': '" + value + "'"), cause
  )

  class NoLangDefException(lineNo: Int) extends ItemCsvParseException(lineNo)

  class InvalidSiteException(
    lineNo: Int, cause: Throwable = null
  ) extends ItemCsvParseException (lineNo, cause = cause)

  class InvalidCategoryException(
    lineNo: Int, cause: Throwable = null
  ) extends ItemCsvParseException (lineNo, cause = cause)

  class InvalidLocaleException(
    lineNo: Int, cause: Throwable = null
  ) extends ItemCsvParseException (lineNo, cause = cause)

  class InvalidTaxException(
    lineNo: Int, cause: Throwable = null
  ) extends ItemCsvParseException (lineNo, cause = cause)

  class InvalidCurrencyException(
    lineNo: Int, cause: Throwable = null
  ) extends ItemCsvParseException (lineNo, cause = cause)

  sealed trait Command
  case object CommentCommand extends Command
  case object LocaleCommand extends Command
  case object ItemCommand extends Command
  object Command {
    def get(s: String): Option[Command] = if (s.isEmpty) Some(CommentCommand) else
      getLongRemovingAfterColonComment(s).flatMap {
        case 0 => Some(LocaleCommand)
        case 1 => Some(ItemCommand)
        case _ => None
      }
  }

  sealed trait Crud
  case object CrudCreate extends Crud
  object Crud {
    def get(s: String): Option[Crud] = s.trim match {
      case "C" => Some(CrudCreate)
      case "c" => Some(CrudCreate)
      case _ => None
    }
  }

  trait CsvLine
  case object CommentCsvLine extends CsvLine
  case class LocaleCsvLine(
    localeId: Long
  ) extends CsvLine
  case class ItemCsvLine(
    crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
    itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
    currencyId: Long, description: String,
    itemNumericMetadata: immutable.Seq[ItemNumericMetadataField],
    itemTextMetadata: immutable.Seq[ItemTextMetadataField],
    siteItemNumericMetadata: immutable.Seq[SiteItemNumericMetadataField],
    siteItemTextMetadata: immutable.Seq[SiteItemTextMetadataField],
    itemPictures: immutable.Seq[String],
    itemDetailPicture: String
  ) extends CsvLine

  def removeAfterColonComment(s: String): String = {
    val idx = s.indexOf(':')
    if (idx == -1) s else s.substring(0, idx)
  }

  def getLongRemovingAfterColonComment(s: String): Option[Long] =
    try {
      Some(removeAfterColonComment(s).toLong)
    }
    catch {
      case e: NumberFormatException => None
    }

  // Returns current locale id.
  def processOneLine(
    lineNo: Int,
    dir: Path, locale: Option[LocaleInfo], z: Iterator[String], conn: Connection,
    toPicPath: (ItemId, Int) => Path, toDetailPicPath: ItemId => Path
  ): Option[LocaleInfo] = {
    readCsvLine(lineNo, z) match {
      case CommentCsvLine => locale
      case LocaleCsvLine(lid) => Some(LocaleInfo.get(lid).getOrElse {
        Logger.error("Invalid locale id = " + lid + ". line " + lineNo)
        throw new InvalidLocaleException(lineNo)
      })
      case item: ItemCsvLine => locale match {
        case None => {
          Logger.error("Locale is not defined. line " + lineNo)
          throw new NoLangDefException(lineNo)
        }
        case Some(loc) =>
          processItem(lineNo, dir, loc, item, conn, toPicPath, toDetailPicPath)
          locale
      }
    }
  }

  def processItem(
    lineNo: Int,
    dir: Path, locale: LocaleInfo, itemCsv: ItemCsvLine, conn: Connection,
    toPicPath: (ItemId, Int) => Path, toDetailPicPath: ItemId => Path
  ) {
    implicit val connection = conn
    val item: Item = try {
      Item.createNew(itemCsv.categoryId)
    }
    catch {
      case e: Throwable => {
        Logger.error("Item CSV error. line " + lineNo + ", record: " + itemCsv)
        throw new InvalidCategoryException(lineNo, e)
      }
    }
    val siteItem: SiteItem = try {
      SiteItem.add(item.id.get, itemCsv.siteId)
    }
    catch {
      case e: Throwable => {
        Logger.error("Invalid site. line " + lineNo + ", record: " + itemCsv)
        throw new InvalidSiteException(lineNo, e)
      }
    }
    if (itemCsv.isCoupon) {
      Coupon.updateAsCoupon(item.id.get)
    }
    val itemName = ItemName.createNew(
      item, immutable.Map(locale -> itemCsv.itemName)
    )
    val itemPrice = ItemPrice.add(item.id.get, itemCsv.siteId)

    CurrencyInfo.get(itemCsv.currencyId).getOrElse {
      Logger.error("Invalid currency. line " + lineNo + ", record: " + itemCsv)
      throw new InvalidCurrencyException(lineNo)
    }

    Tax.get(itemCsv.taxId).getOrElse {
      Logger.error("Invalid tax. line " + lineNo + ", record: " + itemCsv)
      throw new InvalidTaxException(lineNo)
    }

    val itemPriceHistory = ItemPriceHistory.add(
      item.id.get, itemCsv.siteId, itemCsv.taxId, itemCsv.currencyId,
      itemCsv.price, itemCsv.listPrice, itemCsv.costPrice.getOrElse(BigDecimal(0)),
      new org.joda.time.DateTime(Until.Ever)
    )
    val itemDesc = ItemDescription.add(itemCsv.siteId, item.id.get, locale.id, itemCsv.description)
    itemCsv.siteItemNumericMetadata.foreach { md =>
      val siteItemNumericMetadata = SiteItemNumericMetadata.createNew(
        itemCsv.siteId, item.id.get, md.typeCode, md.value, md.until
      )
    }
    itemCsv.siteItemTextMetadata.foreach { md =>
      val siteItemTextMetadata = SiteItemTextMetadata.add(
        item.id.get, itemCsv.siteId, md.typeCode, md.value
      )
    }
    itemCsv.itemPictures.zipWithIndex.foreach { case (picName, idx) =>
      val toPath = toPicPath(item.id.get, idx)
      Files.createDirectories(toPath.getParent)
      Files.copy(dir.resolve(picName), toPath)
    }
    
    val toDetailPath = toDetailPicPath(item.id.get)
    Files.createDirectories(toDetailPath.getParent)
    Files.copy(dir.resolve(itemCsv.itemDetailPicture), toDetailPath)
  }

  def readCsvLine(lineNo: Int, z: Iterator[String]): CsvLine = {
    def init(): TailRec[CsvLine] = {
      val col = z.next()
      Command.get(col) match {
        case None => throw new InvalidColumnException(lineNo, 1, "Command '" + col + "' is invalid", col)
        case Some(cmd) => cmd match {
          case CommentCommand => done(CommentCsvLine)
          case LocaleCommand => tailcall(locale(lineNo, 2))
          case ItemCommand => tailcall(itemCrud(lineNo, 2))
        }
      }
    }

    def locale(lineNo: Int, colNo: Int): TailRec[CsvLine] = {
      val col = z.next()
      getLongRemovingAfterColonComment(col) match {
        case None => throw new InvalidColumnException(lineNo, colNo, "invalid locale", col)
        case Some(localeId) => done(LocaleCsvLine(localeId))
      }
    }

    def itemCrud(lineNo: Int, colNo: Int): TailRec[CsvLine] = {
      val col = z.next()
      Crud.get(col) match {
        case None => throw new InvalidColumnException(lineNo, colNo, "invalid crud", col)
        case Some(crud) => tailcall(itemItemId(lineNo, colNo + 1, crud))
      }
    }

    def itemItemId(lineNo: Int, colNo: Int, crud: Crud): TailRec[CsvLine] = {
      val col = z.next().trim
      val itemId: Option[Long] = to[Long](lineNo, colNo, "itemId", col)
      tailcall(itemSiteId(lineNo, colNo + 1, crud, itemId))
    }

    def itemSiteId(lineNo: Int, colNo: Int, crud: Crud, itemId: Option[Long]): TailRec[CsvLine] = {
      val col = z.next().trim
      val siteId: Long = getLongRemovingAfterColonComment(col).getOrElse {
        throw new InvalidColumnException(lineNo, colNo, "invalid siteId", col)
      }

      tailcall(itemCategoryId(lineNo, colNo + 1, crud, itemId, siteId))
    }

    def itemCategoryId(lineNo: Int, colNo: Int, crud: Crud, itemId: Option[Long], siteId: Long): TailRec[CsvLine] = {
      val col = z.next().trim
      val categoryId: Long = getLongRemovingAfterColonComment(col).getOrElse {
        throw new InvalidColumnException(lineNo, colNo, "invalid categoryId", col)
      }

      tailcall(itemCoupon(lineNo, colNo + 1, crud, itemId, siteId, categoryId))
    }

    def itemCoupon(
      lineNo: Int, colNo: Int, crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long
    ): TailRec[CsvLine] = {
      val col = z.next().trim
      val isCoupon: Boolean = getLongRemovingAfterColonComment(col).getOrElse {
        throw new InvalidColumnException(lineNo, colNo, "invalid coupon", col)
      } match {
        case 0 => false
        case 1 => true
        case _ => throw new InvalidColumnException(lineNo, colNo, "invalid coupon", col)
      }

      tailcall(itemName(lineNo, colNo, crud, itemId, siteId, categoryId, isCoupon))
    }

    def itemName(
      lineNo: Int, colNo: Int, crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean
    ): TailRec[CsvLine] = {
      val col = z.next().trim
      tailcall(itemTaxId(lineNo, colNo + 1, crud, itemId, siteId, categoryId, isCoupon, col))
    }

    def itemTaxId(
      lineNo: Int, colNo: Int, crud: Crud, itemId: Option[Long], siteId: Long,
      categoryId: Long, isCoupon: Boolean, itemName: String
    ): TailRec[CsvLine] = {
      val col = z.next().trim
      val taxId: Long = getLongRemovingAfterColonComment(col).getOrElse {
        throw new InvalidColumnException(lineNo, colNo, "invalid taxId", col)
      }

      tailcall(itemPrice(lineNo, colNo + 1, crud, itemId, siteId, categoryId, isCoupon, itemName, taxId))
    }

    def itemPrice(
      lineNo: Int, colNo: Int, crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long
    ): TailRec[CsvLine] = {
      tailcall(
        itemCostPrice(
          lineNo, colNo + 1, crud, itemId, siteId, categoryId, isCoupon, itemName, taxId,
          toMandatory[BigDecimal](lineNo, colNo, "price", z.next().trim)
        )
      )
    }

    def itemCostPrice(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal
    ): TailRec[CsvLine] = {
      tailcall(
        itemListPrice(
          lineNo, colNo + 1, crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price,
          to[BigDecimal](lineNo, colNo, "cost price", z.next().trim)
        )
      )
    }

    def itemListPrice(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal]
    ): TailRec[CsvLine] = {
      tailcall(
        itemCurrency(
          lineNo, colNo + 1, crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice,
          to[BigDecimal](lineNo, colNo, "list price", z.next().trim)
        )
      )
    }

    def itemCurrency(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal]
    ): TailRec[CsvLine] = {
      val col = z.next().trim
      val currencyId: Long = getLongRemovingAfterColonComment(col).getOrElse {
        throw new InvalidColumnException(lineNo, colNo, "invalid currency id", col)
      }

      tailcall(
        itemDescription(
          lineNo, colNo + 1, crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice, listPrice, currencyId
        )
      )
    }

    def itemDescription(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
      currencyId: Long
    ): TailRec[CsvLine] = {
      tailcall(
        itemNumericMetadata(
          lineNo, colNo + 1,
          crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice, listPrice, currencyId, z.next().trim
        )
      )
    }

    def itemNumericMetadata(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
      currencyId: Long, description: String
    ): TailRec[CsvLine] = {
      val buf = new BufferedReader(new StringReader(z.next().trim))
      @tailrec def parse(result: immutable.Vector[ItemNumericMetadataField]): immutable.Vector[ItemNumericMetadataField] = {
        val line = buf.readLine()
        if (line == null) result else parse(result :+ ItemNumericMetadataField(line))
      }

      tailcall(
        itemTextMetadata(
          lineNo, colNo + 1,
          crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice, listPrice, currencyId, description,
          parse(immutable.Vector())
        )
      )
    }

    def itemTextMetadata(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
        currencyId: Long, description: String,
      itemNumericMetadata: immutable.Seq[ItemNumericMetadataField]
    ): TailRec[CsvLine] = {
      val buf = new BufferedReader(new StringReader(z.next().trim))
      @tailrec def parse(result: immutable.Vector[ItemTextMetadataField]): immutable.Vector[ItemTextMetadataField] = {
        val line = buf.readLine()
        if (line == null) result else parse(result :+ ItemTextMetadataField(line))
      }

      tailcall(
        siteItemNumericMetadata(
          lineNo, colNo + 1,
          crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice, listPrice, currencyId, description,
          itemNumericMetadata, parse(immutable.Vector())
        )
      )
    }

    def siteItemNumericMetadata(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
      currencyId: Long, description: String,
      itemNumericMetadata: immutable.Seq[ItemNumericMetadataField],
      itemTextMetadata: immutable.Seq[ItemTextMetadataField]
    ): TailRec[CsvLine] = {
      val buf = new BufferedReader(new StringReader(z.next().trim))
      @tailrec def parse(result: immutable.Vector[SiteItemNumericMetadataField]): immutable.Vector[SiteItemNumericMetadataField] = {
        val line = buf.readLine()
        if (line == null) result else parse(result :+ SiteItemNumericMetadataField(lineNo, colNo, line))
      }
      tailcall(
        siteItemTextMetadata(
          lineNo, colNo + 1,
          crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice, listPrice, currencyId, description,
          itemNumericMetadata, itemTextMetadata, parse(immutable.Vector())
        )
      )
    }

    def siteItemTextMetadata(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
      currencyId: Long, description: String,
      itemNumericMetadata: immutable.Seq[ItemNumericMetadataField],
      itemTextMetadata: immutable.Seq[ItemTextMetadataField],
      siteItemNumericMetadata: immutable.Seq[SiteItemNumericMetadataField]
    ): TailRec[CsvLine] = {
      val buf = new BufferedReader(new StringReader(z.next().trim))
      @tailrec def parse(result: immutable.Vector[SiteItemTextMetadataField]): immutable.Vector[SiteItemTextMetadataField] = {
        val line = buf.readLine()
        if (line == null) result else parse(result :+ SiteItemTextMetadataField(lineNo, colNo, line))
      }
      tailcall(
        itemPictures(
          lineNo, colNo + 1,
          crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice, listPrice, currencyId, description,
          itemNumericMetadata, itemTextMetadata, siteItemNumericMetadata, parse(immutable.Vector())
        )
      )
    }

    def itemPictures(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
      currencyId: Long, description: String,
      itemNumericMetadata: immutable.Seq[ItemNumericMetadataField],
      itemTextMetadata: immutable.Seq[ItemTextMetadataField],
      siteItemNumericMetadata: immutable.Seq[SiteItemNumericMetadataField],
      siteItemTextMetadata: immutable.Seq[SiteItemTextMetadataField]
    ): TailRec[CsvLine] = {
      val buf = new BufferedReader(new StringReader(z.next().trim))
      @tailrec def parse(result: immutable.Vector[String]): immutable.Vector[String] = {
        val line = buf.readLine()
        if (line == null) result else parse(result :+ line)
      }

      tailcall(
        itemDetailPicture(
          lineNo, colNo + 1,
          crud, itemId, siteId, categoryId, isCoupon, itemName, taxId, price, costPrice, listPrice, currencyId, description,
          itemNumericMetadata, itemTextMetadata, siteItemNumericMetadata, siteItemTextMetadata,
          parse(immutable.Vector())
        )
      )
    }

    def itemDetailPicture(
      lineNo: Int, colNo: Int,
      crud: Crud, itemId: Option[Long], siteId: Long, categoryId: Long, isCoupon: Boolean,
      itemName: String, taxId: Long, price: BigDecimal, costPrice: Option[BigDecimal], listPrice: Option[BigDecimal],
      currencyId: Long, description: String,
      itemNumericMetadata: immutable.Seq[ItemNumericMetadataField],
      itemTextMetadata: immutable.Seq[ItemTextMetadataField],
      siteItemNumericMetadata: immutable.Seq[SiteItemNumericMetadataField],
      siteItemTextMetadata: immutable.Seq[SiteItemTextMetadataField],
      itemPictures: immutable.Vector[String]
    ): TailRec[CsvLine] = {
      done(
        ItemCsvLine(
          crud, itemId, siteId, categoryId, isCoupon,
          itemName, taxId, price, costPrice, listPrice, currencyId, description,
          itemNumericMetadata, itemTextMetadata, siteItemNumericMetadata, siteItemTextMetadata,
          itemPictures, z.next().trim
        )
      )
    }

    init().result
  }
}
