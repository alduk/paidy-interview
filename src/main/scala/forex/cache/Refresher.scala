package forex.cache

import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits._
import forex.client.OneFrameClient
import forex.services.rates.errors.Error.OneFrameClientError
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.FiniteDuration

/**
  * A class that refreshes the forex rates cache at regular intervals.
  *
  * @param client The OneFrameClient instance used to fetch forex rates from the OneFrame API.
  * @param d The duration of the interval at which to refresh the cache.
  * @tparam F The effect type.
  */
class Refresher[F[_]: ConcurrentEffect: Timer](client: OneFrameClient[F], d: FiniteDuration) {

  implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  /**
   * Creates a stream that, when run, refreshes the forex rates cache at regular intervals.
   *
   * This method creates a stream that, at every interval specified by `d` and
   * attempts to refresh the cache by fetching forex rates from the OneFrame API.
   *
   * @return A stream that, when run, refreshes the forex rates cache at regular intervals.
   */
  def refreshStream: fs2.Stream[F, Unit] = {
    val refresher = client
      .refreshCache()
      .handleErrorWith { e =>
        logger.error(e)("Failed to refresh cache!").map(_ => Left(OneFrameClientError(e.getMessage)))
      }
      .flatMap {
        case Right(rates) => logger.debug(s"Cache refreshed with ${rates.size} rates!")
        case Left(e)      => logger.warn(s"Failed to refresh cache!\n$e")
      }
    evalEvery(d)(logger.info(s"Refreshing cache") *> refresher)
  }
}
