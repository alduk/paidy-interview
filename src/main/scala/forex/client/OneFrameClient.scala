package forex.client

import cats.effect.Resource
import cats.implicits._
import forex.client.OneFrameClient._
import forex.config.OneFrameConfig
import forex.domain.Rate.Pair
import org.http4s.Method.GET
import org.http4s._
import org.http4s.client.Client

class OneFrameClient[F[_]](uri: Uri, token: String, client: Client[F]) {
  def getRates(pairs: Seq[Pair]): Resource[F, Response[F]] = {
    val r = (uri / "rates").withMultiValueQueryParams(Map("pair" -> pairs))
    val request = Request[F](method = GET, uri = r, headers = Headers("token" -> token))
    client.run(request)
  }
}

object OneFrameClient {

  def apply[F[_]](config: OneFrameConfig, client: Client[F]): Either[ParseFailure, OneFrameClient[F]] =
    Uri
      .fromString(config.uri)
      .map(new OneFrameClient[F](_, config.token, client))

  implicit val pairParamEncoder: QueryParamEncoder[Pair] =
    (pair: Pair) => QueryParameterValue(s"""${pair.from.show}${pair.to.show}""")

}
