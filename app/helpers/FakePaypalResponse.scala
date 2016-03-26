package helpers

import play.api.libs.ws.{WSResponse, WSCookie}
import play.api.libs.json._
import scala.xml.Elem

object FakePaypalResponse {
  val FakePaypalResponsBody: () => String = Cache.config(
    _.getString("fakePaypalRespons.body").getOrElse(
      throw new IllegalStateException("Specify fakePaypalRespons.body in configuration.")
    )
  )
  val FakePaypalResponsStatusCode: () => Int = Cache.config(
    _.getInt("fakePaypalRespons.statusCode").getOrElse(
      throw new IllegalStateException("Specify fakePaypalRespons.statusCode in configuration.")
    )
  )

  def apply(): FakePaypalResponse = FakePaypalResponse(
    FakePaypalResponsBody(),
    FakePaypalResponsStatusCode()
  )
}

case class FakePaypalResponse(
  body: String,
  status: Int
) extends WSResponse {
  def allHeaders: Map[String, Seq[String]] = Map()
  def bodyAsBytes: Array[Byte] = body.getBytes
  def cookie(name: String): Option[WSCookie] = None
  def cookies: Seq[WSCookie] = Seq()
  def header(key: String): Option[String] = None
  def json: JsValue = JsNull
  def statusText: String = ""
  def underlying[T] = (new AnyRef).asInstanceOf[T]
  def xml: Elem = <resp/>
}

