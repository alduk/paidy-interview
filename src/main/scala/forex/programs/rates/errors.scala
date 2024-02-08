package forex.programs.rates

import forex.services.rates.errors.{Error => RatesServiceError}
import io.circe.{Encoder, Json}
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder
import io.circe.generic.semiauto.deriveEncoder

object errors {

  sealed trait Error extends Exception
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
    final case class RateClientError(msg: String) extends Error
    final case class RateServerError(msg: String) extends Error
    final case class RateDecodingError(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.OneFrameClientError(msg) => Error.RateClientError(msg)
    case RatesServiceError.OneFrameServerError(msg) => Error.RateServerError(msg)
    case RatesServiceError.OneFrameDecodingError(msg) => Error.RateDecodingError(msg)
  }
  case class ErrorsDto(errors: List[ErrorDto]) extends AnyVal
  case class ErrorDto(code: String, message: String, path: String, details: Option[Json] = None)

  object ErrorsDto {
    def apply(error: ErrorDto): ErrorsDto = ErrorsDto(List(error))
  }
  object ErrorCodecs {
    implicit val errorDtoEncoder: Encoder[ErrorDto] = deriveEncoder
    implicit val errorsDtoEncoder: Encoder[ErrorsDto] = deriveUnwrappedEncoder
  }

}
