package helpers

import java.security.MessageDigest
import com.google.common.primitives.Longs

object PasswordHash {
  def generate(password: String, salt: Long) = {
    val md = createSha1Encoder
    md.update(Longs.toByteArray(salt));
    md.update(password.getBytes("utf-8"))
    Longs.fromByteArray(md.digest())
  }

  def createSha1Encoder = MessageDigest.getInstance("SHA-1")
}
