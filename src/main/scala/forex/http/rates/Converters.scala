package forex.http.rates

import forex.domain._

object Converters {
  import Protocol._

  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }

  implicit class RateOps(val response: GetApiResponse) extends AnyVal {
    def asRate: Rate =
      Rate(
        pair = Rate.Pair(response.from, response.to),
        price = response.price,
        timestamp = response.timestamp
      )
  }

}
