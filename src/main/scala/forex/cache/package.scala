package forex

import cats.effect.Timer
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

package object cache {

  /**
    * Creates a stream that, when run, evaluates the given effect at regular intervals.
    *
    * @param d The duration of the interval at which to evaluate the effect.
    * @param fa The effect to evaluate.
    * @tparam A The type of the result of the effect.
    * @return A stream that, when run, evaluates the given effect at regular intervals.
    */
  def evalEvery[F[_]: Timer, A](d: FiniteDuration)(fa: F[A]): Stream[F, A] =
    (Stream.eval(fa) ++ Stream.sleep_[F](d)).repeat
}
