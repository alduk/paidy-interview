package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrame: OneFrameConfig,
    cacheTtl: FiniteDuration
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameConfig(
    uri: String,
    token: String
)