package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import forex.client.OneFrameClient
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http._
import forex.http.rates.Converters._
import forex.http.rates.Protocol._
import forex.services.rates.Algebra
import forex.services.rates.errors.Error._
import forex.services.rates.errors._
import org.http4s.Status.{ClientError, ServerError, Successful}

class OneFrameLive[F[_]: Sync](oneFrameClient: OneFrameClient[F]) extends Algebra[F] {
  override def get(pair: Pair): F[Error Either Rate] = {
    oneFrameClient.getRates(Seq(pair)).use {
      case Successful(response) =>
        response.as[List[GetApiResponse]].attempt.map {
          case Right(response) =>
            response.map(_.asRate).headOption
              .map(Right.apply)
              .getOrElse(Left(OneFrameLookupFailed("No rate found")))
          case Left(e) => Left(OneFrameDecodingError(s"Failed to decode response: ${e.getMessage}"))
        }
      case ClientError(response) => response.as[OneFrameClientError].map(Left(_))
      case ServerError(response) => response.as[OneFrameServerError].map(Left(_))
      case r =>
        r.as[String]
          .map(b => Left(OneFrameLookupFailed(s"Request failed with status ${r.status.code} and body $b")))
    }
  }
}
