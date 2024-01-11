package com.finance

import cats.effect.*
import cats.syntax.all.*
import com.finance.validate.CustomerInfo.{addCustomer, fetchCustomer}
import com.finance.Initialise.{initialiseDb, given}
import com.finance.Kafka.Transactions.consumeTransactions
import com.finance.Models.UserEntities.{ClientErrorMessage, Customer, ExistingClient}
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.generic.auto.*

import scala.concurrent.ExecutionContext

object Entrypoint extends IOApp {

  val getUsers
      : PublicEndpoint[String, ClientErrorMessage, ExistingClient, Any] =
    endpoint.get
      .errorOut(jsonBody[ClientErrorMessage])
      .in("customers" / "get")
      .in(query[String]("userid"))
      .out(jsonBody[ExistingClient])

  val createUser: PublicEndpoint[Customer, ClientErrorMessage, Int, Any] =
    endpoint.post
      .errorOut(jsonBody[ClientErrorMessage])
      .in("customer" / "add")
      .in(
        jsonBody[Customer]
          .description("New Customer")
      )
      .out(jsonBody[Int])

  val health: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get
      .in("")
      .out(jsonBody[String])

  // server-side logic
  implicit val ec: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  val addCustomerRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes {
      createUser.serverLogic { user =>
        for {
          res <- addCustomer(user)
        } yield res
      }
    }

  val getUsersRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes {
      getUsers.serverLogic { userId =>
        fetchCustomer(userId)

      }
    }

  val getHealthRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes {
      health.serverLogic { _ =>
        IO.pure(Right("OK"))

      }
    }

  val swaggerUIRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(
      SwaggerInterpreter().fromEndpoints[IO](
        List(getUsers, createUser),
        "The tapir library",
        "1.0.0"
      )
    )

  val routes: HttpRoutes[IO] =
    getHealthRoutes <+> getUsersRoutes <+> addCustomerRoutes <+> swaggerUIRoutes

  override def run(args: List[String]): IO[ExitCode] = {
    // starting the server
    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(Router("/" -> (routes)).orNotFound)
      .resource
      .use { _ =>
        initialiseDb() >> IO.pure {
          println{
            """||||||||||||           |||          |||||   ||||   |||  |||
               ||||     |||         ||| |||        ||||||| ||||   ||| |||
               ||||||||||||       ||||  |||||      |||| |||||||   |||||
               ||||     |||      |||||||||||||     ||||  ||||||   ||| |||
               ||||||||||||     |||||     |||||    ||||    ||||   |||  |||""".stripMargin

          }

        } >> IO.pure(
          println("API started and accessible on port 8080")
        ) >> consumeTransactions().start
        >> IO.never.as(ExitCode.Success)

      }

  }
}
