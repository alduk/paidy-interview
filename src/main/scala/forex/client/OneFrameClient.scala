package forex.client

import cats.effect.Sync
import cats.implicits._
import forex.client.OneFrameClient._
import forex.config.OneFrameConfig
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http._
import forex.http.rates.Converters._
import forex.http.rates.Protocol.{GetApiResponse, _}
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error._
import io.chrisdavenport.mules.Cache
import org.http4s.Method.GET
import org.http4s.Status.{ClientError, ServerError, Successful}
import org.http4s._
import org.http4s.client.Client

/**
 * A class that interacts with the OneFrame API to fetch forex rates and cache them.
 *
 * @param uri The base URI of the OneFrame API.
 * @param token The authentication token for the OneFrame API.
 * @param client The HTTP client to use for making requests to the OneFrame API.
 * @param cache The cache to store the fetched forex rates.
 * @tparam F The effect type.
 */
class OneFrameClient[F[_]: Sync](uri: Uri, token: String, client: Client[F], cache: Cache[F, Rate.Pair, Rate]) {

  /**
   * Fetches all forex rates from the OneFrame API and caches them.
   *
   * @return A list of all forex rates, or an error if the fetch operation fails.
   */
  def refreshCache(): F[Either[Error, List[Rate]]] =
    getRates(Pair.all).flatMap { result =>
      result.map { rates =>
        rates.map(rate => cache.insert(rate.pair, rate)).sequence.as(rates)
      }.sequence
    }

  /**
   * Fetches forex rates for the given pairs of currencies from the OneFrame API.
   *
   * @param pairs The pairs of currencies for which to fetch forex rates.
   * @return A list of forex rates for the given pairs of currencies, or an error if the fetch operation fails.
   */
  def getRates(pairs: Seq[Pair]): F[Error Either List[Rate]] = {
    val r       = (uri / "rates").withMultiValueQueryParams(Map("pair" -> pairs))
    val request = Request[F](method = GET, uri = r, headers = Headers("token" -> token))
    client.run(request).use {
      case Successful(response) =>
        response.as[List[GetApiResponse]].attempt.map {
          case Right(response) => response.map(_.asRate).asRight[Error]
          case Left(e)         => Left(OneFrameDecodingError(s"Failed to decode response: ${e.getMessage}"))
        }
      case ClientError(response) => response.as[OneFrameClientError].map(Left(_))
      case ServerError(response) => response.as[OneFrameServerError].map(Left(_))
      case r =>
        r.as[String]
          .map(b => Left(OneFrameLookupFailed(s"Request failed with status ${r.status.code} and body $b")))
    }
  }
}

object OneFrameClient {

  def apply[F[_]: Sync](config: OneFrameConfig,
                        client: Client[F],
                        cache: Cache[F, Rate.Pair, Rate]): Either[ParseFailure, OneFrameClient[F]] =
    Uri
      .fromString(config.uri)
      .map(new OneFrameClient[F](_, config.token, client, cache))

  implicit val pairParamEncoder: QueryParamEncoder[Pair] =
    (pair: Pair) => QueryParameterValue(s"""${pair.from.show}${pair.to.show}""")

}
