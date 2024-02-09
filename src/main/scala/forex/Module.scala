package forex

import cats.data.Kleisli
import cats.effect.{Concurrent, Timer}
import cats.implicits._
import forex.config.ApplicationConfig
import forex.domain.Rate
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.programs.rates.errors.ErrorCodecs._
import forex.programs.rates.errors._
import forex.services._
import io.chrisdavenport.mules.Cache
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, cache: Cache[F, Rate.Pair, Rate]) {

  private val ratesService: RatesService[F] = RatesServices.live(cache)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val errorsMiddleware: TotalMiddleware = { app: HttpApp[F] =>
    val dsl = Http4sDsl[F]
    import dsl._

    Kleisli { (request: Request[F]) =>
      app.run(request).recoverWith {
        case error: Error =>
          error match {
            case Error.RateLookupFailed(msg) =>
              NotFound(ErrorDto("RateLookupFailed", msg, request.pathInfo.renderString).asJson)
            case Error.RateClientError(msg) =>
              InternalServerError(ErrorDto("RateClientError", msg, request.pathInfo.renderString).asJson)
            case Error.RateServerError(msg) =>
              InternalServerError(ErrorDto("RateServerError", msg, request.pathInfo.renderString).asJson)
            case Error.RateDecodingError(msg) =>
              BadRequest(ErrorDto("RateDecodingError", msg, request.pathInfo.renderString).asJson)
          }
        case error: Throwable =>
          InternalServerError(ErrorDto("InternalServerError", error.getMessage, request.pathInfo.renderString).asJson)
      }
    }
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = errorsMiddleware(appMiddleware(routesMiddleware(http).orNotFound))

}
