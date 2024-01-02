package com.finance

import cats.effect.*
import cats.syntax.all.*
import com.finance.validate.CustomerInfo.{addCustomer, fetchCustomer}
import com.finance.Initialise.{initialiseDb, given}
import doobie.Fragment
import doobie.implicits.*
import cats.effect.unsafe.implicits.global

import scala.io.Source
import com.finance.Models.UserEntities.{
  ClientErrorMessage,
  Customer,
  ExistingClient
}
import doobie.hikari.HikariTransactor
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

  val swaggerUIRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(
      SwaggerInterpreter().fromEndpoints[IO](
        List(getUsers, createUser),
        "The tapir library",
        "1.0.0"
      )
    )

  val routes: HttpRoutes[IO] =
    getUsersRoutes <+> addCustomerRoutes <+> swaggerUIRoutes

  override def run(args: List[String]): IO[ExitCode] = {
    // starting the server
    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(Router("/" -> (routes)).orNotFound)
      .resource
      .use { _ =>
        IO {
          initialiseDb().unsafeRunSync()
          println("Go to: http://localhost:8080/docs")
          println(s"Press any key to exit ...")
          scala.io.StdIn.readLine()
        }
      }
      .as(ExitCode.Success)
  }
}
