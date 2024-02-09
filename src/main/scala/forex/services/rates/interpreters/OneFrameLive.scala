package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.services.rates.Algebra
import forex.services.rates.errors.Error._
import forex.services.rates.errors._
import io.chrisdavenport.mules.Cache

/**
 * A class that provides live forex rates using a cache.
 *
 * @param cache A cache that maps a pair of currencies to their exchange rate.
 * @tparam F The effect type.
 */
class OneFrameLive[F[_]: Sync](cache: Cache[F, Rate.Pair, Rate]) extends Algebra[F] {

  /**
   * Retrieves the exchange rate for a given pair of currencies from the cache.
   *
   * If the rate is not found in the cache, it returns an error.
   *
   * @param pair The pair of currencies for which to retrieve the exchange rate.
   * @return The exchange rate for the given pair of currencies, or an error if the rate is not found in the cache.
   */
  override def get(pair: Pair): F[Error Either Rate] =
    cache.lookup(pair)
      .map(_.toRight[Error](OneFrameLookupFailed(s"Failed to find rate for $pair")))

}
