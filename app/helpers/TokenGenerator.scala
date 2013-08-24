package helpers

import java.security.SecureRandom
import java.nio.ByteBuffer

trait TokenGenerator {
  def next: Long
}

class RandomTokenGenerator(private val source: SecureRandom = new SecureRandom) extends TokenGenerator {
  override def next: Long =
    ByteBuffer.wrap(nextRandom(new Array[Byte](8))).asLongBuffer().get

  def nextRandom(outBuf: Array[Byte]): Array[Byte] = {
    source.nextBytes(outBuf)
    outBuf
  }
}

object RandomTokenGenerator {
  private val instance = new RandomTokenGenerator
  def apply() = instance
}

