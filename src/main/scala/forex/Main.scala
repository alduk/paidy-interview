package forex

import cats.effect._
import forex.client.OneFrameClient
import forex.config._
import forex.domain.Rate
import forex.domain.Rate.Pair
import fs2.Stream
import io.chrisdavenport.mules._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- Stream
                 .resource(BlazeClientBuilder[F](ec).resource)
                 .map(c => OneFrameClient(config.oneFrame, c))
                 .flatMap(Stream.fromEither[F](_))
      cache <- Stream.eval(
                MemoryCache.ofSingleImmutableMap[F, Pair, Rate](
                  Some(TimeSpec.unsafeFromDuration(config.cacheTtlInMinutes.minutes))
                )
              )
      module = new Module[F](config, client, cache)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
