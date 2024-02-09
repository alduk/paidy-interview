package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.client.OneFrameClient
import forex.domain.Rate
import forex.services.rates.interpreters._
import io.chrisdavenport.mules.Cache

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Sync](oneFrameClient: OneFrameClient[F], cache: Cache[F, Rate.Pair, Rate]): Algebra[F] =
    new OneFrameLive[F](oneFrameClient, cache)
}
