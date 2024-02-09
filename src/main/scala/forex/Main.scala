package forex

import cats.effect._
import forex.cache.Refresher
import forex.client.OneFrameClient
import forex.config._
import forex.domain.Rate
import forex.domain.Rate.Pair
import fs2.Stream
import io.chrisdavenport.mules._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  implicit val logger: Logger[F] = Slf4jLogger.getLogger[F]

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      cache <- Stream.eval(
                MemoryCache.ofSingleImmutableMap[F, Pair, Rate](
                  Some(TimeSpec.unsafeFromDuration(config.cacheTtl))
                )
              )
      client <- Stream
                 .resource(BlazeClientBuilder[F](ec).resource)
                 .map(client => OneFrameClient(config.oneFrame, client, cache))
                 .flatMap(Stream.fromEither[F](_))
      module = new Module[F](config, cache)
      refresher = new Refresher(client, config.cacheTtl)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
            .concurrently(refresher.refreshStream)
    } yield ()

}
