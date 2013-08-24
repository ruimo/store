import helpers.{RandomTokenGenerator, TokenGenerator}

package object controllers {
  implicit val tokenGenerator: TokenGenerator = RandomTokenGenerator()
}
