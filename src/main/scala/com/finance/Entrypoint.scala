package com.finance

import cats.effect.*
import cats.syntax.all.*
import com.finance.validate.CustomerInfo.{checkCustomer, fetchCustomer}
import com.finance.Initialise.given
import com.finance.Models.Decoders.{Customer, User}
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

  val getUsers: PublicEndpoint[String, Unit, User, Any] = endpoint.get
    .in("customers" / "get")
    .in(query[String]("userid"))
    .out(jsonBody[User])


  val createUser: PublicEndpoint[Customer, Unit, String, Any] = endpoint.post
    .in("customer" / "add")
    .in(
      jsonBody[Customer]
        .description("New Customer")
    )
    .out(jsonBody[String])

  // server-side logic
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global


  val addCustomerRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes{
      createUser.serverLogicSuccess { user =>
        for {
          res <- checkCustomer(user)
        } yield res

      }
      }

  val getUsersRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes{
      getUsers.serverLogicSuccess { userId =>
        fetchCustomer(userId)

      }}
  // generating and exposing the documentation in yml
  val swaggerUIRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(
      SwaggerInterpreter().fromEndpoints[IO](List(getUsers, createUser), "The tapir library", "1.0.0")
    )

  val routes: HttpRoutes[IO] = getUsersRoutes <+> addCustomerRoutes <+> swaggerUIRoutes


  override def run(args: List[String]): IO[ExitCode] = {
    // starting the server
    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(Router("/" -> (routes)).orNotFound)
      .resource
      .use { client =>
        IO {
          println("Go to: http://localhost:8080/docs")
          println("Press any key to exit ...")
          scala.io.StdIn.readLine()
        }
      }
      .as(ExitCode.Success)
  }
}