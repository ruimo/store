package helpers

import org.specs2.mutable._
import java.security.SecureRandom

class TokenGeneratorSpec extends Specification {
  "TokenGenerator" should {
    "nextRandom calls SecureRandom.nextBytes." in {
      new RandomTokenGenerator(new SecureRandom {
        override def nextBytes(ar: Array[Byte]) {
          ar(0) = 0x01.asInstanceOf[Byte]
          ar(1) = 0x02.asInstanceOf[Byte]
          ar(2) = 0x03.asInstanceOf[Byte]
          ar(3) = 0x04.asInstanceOf[Byte]
          ar(4) = 0x80.asInstanceOf[Byte]
          ar(5) = 0x81.asInstanceOf[Byte]
          ar(6) = 0x82.asInstanceOf[Byte]
          ar(7) = 0x83.asInstanceOf[Byte]
        }
      }).nextRandom(new Array[Byte](8)) === Array[Byte](
        0x01, 0x02, 0x03, 0x04,
        0x80.asInstanceOf[Byte], 0x81.asInstanceOf[Byte],
        0x82.asInstanceOf[Byte], 0x83.asInstanceOf[Byte]
      )
    }
  }

  "next returns long value created from nextRandom." in {
      new RandomTokenGenerator(new SecureRandom {
        override def nextBytes(ar: Array[Byte]) {
          ar(0) = 0x80.asInstanceOf[Byte]
          ar(1) = 0x81.asInstanceOf[Byte]
          ar(2) = 0x82.asInstanceOf[Byte]
          ar(3) = 0x83.asInstanceOf[Byte]
          ar(4) = 0x01.asInstanceOf[Byte]
          ar(5) = 0x02.asInstanceOf[Byte]
          ar(6) = 0x03.asInstanceOf[Byte]
          ar(7) = 0x04.asInstanceOf[Byte]
        }
      }).next === 0x8081828301020304L
  }
}
