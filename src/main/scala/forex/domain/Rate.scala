package forex.domain

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  object Pair {
    val all = (Currency.all.combinations(2) ++ Currency.all.reverse.combinations(2)).collect {
      case List(from, to) => Pair(from, to)
    }.toSeq
  }
}
