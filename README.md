Added implementation of the Algebra trait for OneFrame [OneFrameLive](src/main/scala/forex/services/rates/interpreters/OneFrameLive.scala)

To implement this live Algebra, we need to make an HTTP request to the OneFrame API. 
We will use the http4s-client library to make the request and wrapper implemented in [OneFrameClient](src/main/scala/forex/client/OneFrameClient.scala).
Configuration for the client is provided in [OneFrameConfig](src/main/scala/forex/config/ApplicationConfig.scala).

For caching support, we will use the [mule](https://github.com/davenverse/mules) library.
ttl for cache added in [ApplicationConfig](src/main/scala/forex/config/ApplicationConfig.scala).
The cache and client initialization is done in [Main](src/main/scala/forex/Main.scala) , 
they are used in [OneFrameLive](src/main/scala/forex/services/rates/interpreters/OneFrameLive.scala).

Sample error handling added to [Module](src/main/scala/forex/Module.scala).