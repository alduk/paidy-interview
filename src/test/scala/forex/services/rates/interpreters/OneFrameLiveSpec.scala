package forex.services.rates.interpreters

import cats.effect._
import forex.domain.Currency.{EUR, JPY, USD}
import forex.domain.Rate.Pair
import forex.domain.{Price, Rate, Timestamp}
import forex.services.rates.errors.Error.OneFrameLookupFailed
import io.chrisdavenport.mules.{MemoryCache, TimeSpec}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class OneFrameLiveSpec extends AnyFlatSpec with Matchers {
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  "OneFrameLive" should "return the rate if it exists in the cache" in {
    val pair = Pair(USD, EUR)
    val rate = Rate(pair, Price(1.2), Timestamp.now)
    val cache =
      MemoryCache.ofSingleImmutableMap[IO, Rate.Pair, Rate](Some(TimeSpec.unsafeFromDuration(1.hour))).unsafeRunSync()
    cache.insert(pair, rate).unsafeRunSync()
    val oneFrameLive = new OneFrameLive[IO](cache)

    val result = oneFrameLive.get(pair).unsafeRunSync()

    result should be(Right(rate))
  }

  it should "return an error if the rate does not exist in the cache" in {
    val pair = Pair(USD, JPY)
    val cache =
      MemoryCache.ofSingleImmutableMap[IO, Rate.Pair, Rate](Some(TimeSpec.unsafeFromDuration(1.hour))).unsafeRunSync()
    val oneFrameLive = new OneFrameLive[IO](cache)

    val result = oneFrameLive.get(pair).unsafeRunSync()

    result should be(Left(OneFrameLookupFailed(s"Failed to find rate for $pair")))
  }
}
