Added implementation of the Algebra trait for OneFrame [OneFrameLive](src/main/scala/forex/services/rates/interpreters/OneFrameLive.scala)

To implement this live Algebra, we need to make an HTTP request to the OneFrame API.<br>
We will use the http4s-client library to make the request and wrapper implemented in [OneFrameClient](src/main/scala/forex/client/OneFrameClient.scala).
Configuration for the client is provided in [OneFrameConfig](src/main/scala/forex/config/ApplicationConfig.scala).

For caching support, we will use the [mule](https://github.com/davenverse/mules) library.<br>
cache-ttl configuration parameter for cache added in [ApplicationConfig](src/main/scala/forex/config/ApplicationConfig.scala).<br>
To  avoid cache stampede, we will refresh cache in separate concurrent fs2 stream declared in [Refresher](src/main/scala/forex/cache/Refresher.scala) using OneFrame API feature to retrieve multiple rates at once.

The cache and client initialization is done in [Main](src/main/scala/forex/Main.scala) .<br>
Cache is used in [OneFrameLive](src/main/scala/forex/services/rates/interpreters/OneFrameLive.scala).<br>
Cash and client are used in [Refresher](src/main/scala/forex/cache/Refresher.scala).



Sample error handling and logging added to application.