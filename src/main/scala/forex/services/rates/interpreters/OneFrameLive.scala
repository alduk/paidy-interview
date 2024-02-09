package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import forex.client.OneFrameClient
import forex.domain.Rate.Pair
import forex.domain.{Currency, Rate}
import forex.http._
import forex.http.rates.Converters._
import forex.http.rates.Protocol._
import forex.services.rates.Algebra
import forex.services.rates.errors.Error._
import forex.services.rates.errors._
import io.chrisdavenport.mules.Cache
import org.http4s.Status._

/**
  * A live implementation of the Algebra trait for OneFrame.
  * @param oneFrameClient The client used to interact with the OneFrame API.
  * @param cache The cache used to store rate pairs.
  */
class OneFrameLive[F[_]: Sync](oneFrameClient: OneFrameClient[F], cache: Cache[F, Rate.Pair, Rate]) extends Algebra[F] {

  /**
    * Retrieves a rate pair from the cache or refreshes the cache if the pair is not found.
    * @param pair The rate pair to retrieve.
    * @return Either an Error or the requested Rate.
    */
  override def get(pair: Pair): F[Error Either Rate] =
    for {
      rate <- cache.lookup(pair)
      result <- processCacheLookup(pair, rate)
    } yield result

  /**
    * Processes the result of a cache lookup.
    * @param pair The rate pair that was looked up.
    * @param rate The result of the cache lookup.
    * @return Either an Error or the requested Rate.
    */
  private def processCacheLookup(pair: Pair, rate: Option[Rate]): F[Either[Error, Rate]] =
    rate match {
      case Some(r) => r.asRight[Error].pure[F]
      case None =>
        refreshCache().flatMap {
          case Right(_) =>
            cache.lookup(pair).map(_.toRight[Error](OneFrameLookupFailed(s"Failed to find rate for $pair")))
          case Left(e) => e.asLeft[Rate].pure[F]
        }
    }

  /**
    * Refreshes the cache with all possible rate pairs.
    * @param pair The rate pair that triggered the cache refresh.
    * @return Either an Error or a list of all Rates.
    */
  private def refreshCache(): F[Either[Error, List[Rate]]] = {
    val allPairs = (Currency.all.combinations(2) ++ Currency.all.reverse
                    .combinations(2)).collect { case List(from, to) => Pair(from, to) }.toSeq
    getFromService(allPairs).flatMap { result =>
      result.map { rates =>
        rates.map(rate => cache.insert(rate.pair, rate)).sequence.as(rates)
      }.sequence
    }
  }

  /**
    * Retrieves a list of rates from the OneFrame service.
    * @param pairs The list of rate pairs to retrieve.
    * @return Either an Error or a list of Rates.
    */
  def getFromService(pairs: Seq[Pair]): F[Error Either List[Rate]] =
    oneFrameClient.getRates(pairs).use {
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
