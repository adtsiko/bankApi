import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.finance.Query.postgres.insertRegistrationQuery
import com.finance.Models.UserModels.{Address, Occupation, ExistingClient, createNewClient, UserRegistrationBody}
import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import doobie.ExecutionContexts
import doobie.implicits.*
import doobie.hikari.HikariTransactor
import doobie.implicits.toSqlInterpolator
import com.finance.Models.UserModels.Occupation
import com.finance.Initialise.initialisePostgresDb
import cats.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

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

  implicit val  logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "addNewUser" should "insert a new user into the database" in {

    val newUser = createNewClient(
      UserRegistrationBody(
          "Mr",
          "James",
          "Patrick",
          "james_stpatrick@gmail.com",
          44749109413L,
          Address(
            "15 Hatton Gardens",
            "LONDON",
            "EC1N8JT"
          ),
         Occupation(
            "Data Engineer",
            90374,
            "Energy"
          ),
        "Married",
       true,
        53
      )
    )

    val b = for {
      _ <- initialisePostgresDb()
      _ <- insertRegistrationQuery(newUser._1, newUser._3, newUser._2)(using tranAc)
      user <- tranAc.use { xa =>
        sql"SELECT * FROM users WHERE userid = '58bbc77a-b7cb-5c65-9fe2-9c253f745996'"
          .query[ExistingClient]
          .option
          .transact(xa)
      }

    } yield user

    // Extract the value from the Resource and perform the comparison
    val result: ExistingClient =
      b.unsafeRunSync().getOrElse(fail("User not found"))
    result shouldBe ExistingClient(
      userId = "58bbc77a-b7cb-5c65-9fe2-9c253f745996",
      title = "Mr",
      firstName = "James",
      lastName = "Patrick",
      emailAddress = "james_stpatrick@gmail.com",
      age = 53
    )
  }
}
