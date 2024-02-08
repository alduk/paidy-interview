package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.client.OneFrameClient
import forex.services.rates.interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Sync](oneFrameClient: OneFrameClient[F]): Algebra[F] = new OneFrameLive[F](oneFrameClient)
}
