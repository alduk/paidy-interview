package forex.http.rates

import cats.effect.IO
import forex.domain._
import forex.programs.RatesProgram
import forex.programs.rates.Protocol.GetRatesRequest
import org.http4s._
import org.http4s.implicits._
import org.mockito.Mockito._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar

class RatesHttpRoutesSpec extends AnyFreeSpec with MockitoSugar {

  "RatesHttpRoutes" - {

    "GET /rates" - {

      "should return OK when rates are successfully fetched" in {
        val ratesProgram = mock[RatesProgram[IO]]
        when(ratesProgram.get(GetRatesRequest(Currency.EUR, Currency.USD)))
          .thenReturn(IO.pure(Right(Rate(Rate.Pair(Currency.EUR, Currency.USD), Price(BigDecimal(1.2)), Timestamp.now))))

        val routes = new RatesHttpRoutes[IO](ratesProgram).routes.orNotFound

        val request = Request[IO](Method.GET, uri"/rates?from=EUR&to=USD")
        val response = routes.run(request).unsafeRunSync()

        assert(response.status == Status.Ok)
      }
    }
  }
}