package forex.services.rates

object errors {

  sealed trait Error
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class OneFrameClientError(msg: String) extends Error
    final case class OneFrameServerError(msg: String) extends Error
    final case class OneFrameDecodingError(msg: String) extends Error
  }

}
