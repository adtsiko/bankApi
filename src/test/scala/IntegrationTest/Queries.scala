import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.finance.Query.client.addNewUser
import com.finance.Models.UserEntities.{
  Customer,
  ExistingClient,
  createNewClient
}
import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import com.finance.Initialise.initialiseDb
import doobie.ExecutionContexts
import doobie.implicits.*
import doobie.hikari.HikariTransactor
import doobie.implicits.toSqlInterpolator

class UserManagerQueries
    extends AnyFlatSpec
    with ForAllTestContainer
    with Matchers {

  override val container: PostgreSQLContainer = PostgreSQLContainer()
  val dbName = "test-finance"
  val dbUsername = "test"
  val dbPassword = "test"

  implicit val tranAc: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.postgresql.Driver",
        url = container.jdbcUrl,
        user = dbUsername,
        pass = dbPassword,
        connectEC = ce
      )
    } yield xa

  "addNewUser" should "insert a new user into the database" in {

    val newUser = createNewClient(
      Customer(
        "James",
        "Patrick",
        "james_stpatrick@gmail.com",
        "15 Hatton Gardens",
        "London",
        "EC1N8JT",
        24,
        700
      )
    )

    val b = for {
      _ <- initialiseDb()
      _ <- addNewUser(newUser)(using tranAc)
      user <- tranAc.use { xa =>
        sql"SELECT * FROM users WHERE userid = 'f483caa6-0597-5581-a3dd-8218ecd2ff86'"
          .query[ExistingClient]
          .option
          .transact(xa)
      }

    } yield user

    // Extract the value from the Resource and perform the comparison
    val result: ExistingClient =
      b.unsafeRunSync().getOrElse(fail("User not found"))
    result shouldBe ExistingClient(
      userId = "f483caa6-0597-5581-a3dd-8218ecd2ff86",
      name = "James Patrick",
      emailAddress = "james_stpatrick@gmail.com",
      region = "London",
      age = 24,
      creditScore = 700
    )
  }
}
