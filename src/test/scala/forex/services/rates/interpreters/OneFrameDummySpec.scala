package forex.services.rates.interpreters

import cats.effect.IO
import forex.domain.{ Currency, Price, Rate, Timestamp }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OneFrameDummySpec extends AnyFlatSpec with Matchers {
  val defaultPrice = Price(BigDecimal(100))

  "OneFrameDummy" should "return a rate for a given pair" in {
    val service = new OneFrameDummy[IO]
    val pair    = Rate.Pair(Currency.EUR, Currency.USD)
    val result  = service.get(pair).unsafeRunSync()

    result.isRight shouldBe true
    result.map(_.pair) shouldBe Right(pair)
    result.map(_.price) shouldBe Right(defaultPrice)
  }

  it should "return a rate with a current timestamp" in {
    val service = new OneFrameDummy[IO]
    val pair    = Rate.Pair(Currency.EUR, Currency.USD)
    val result  = service.get(pair).unsafeRunSync()

    result.isRight shouldBe true
    val now = Timestamp.now
    result.foreach(_.timestamp.value should be <= now.value)
  }

  it should "return a rate for any currency pair" in {
    val service = new OneFrameDummy[IO]
    Currency.all.combinations(2).foreach {
      case List(from, to) =>
        val pair   = Rate.Pair(from, to)
        val result = service.get(pair).unsafeRunSync()

        result.isRight shouldBe true
        result.map(_.pair) shouldBe Right(pair)
        result.map(_.price) shouldBe Right(defaultPrice)
      case _ =>
    }
  }
}
