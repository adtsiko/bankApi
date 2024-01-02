package com.finance.validate

import cats.effect.{IO, *}
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import io.circe.{Decoder, HCursor, Json}
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import com.finance.Models.Decoders.{PostCodeValidate, addressDecoder}
import com.finance.Models.UserEntities.{
  ClientErrorMessage,
  Customer,
  ExistingClient,
  createNewClient
}
import com.finance.Query.client.{addNewUser, fetchUser}
import com.github.f4b6a3.uuid.UuidCreator
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.Logger

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

object CustomerInfo {

  /** Validates the postcode using the postcode API
    * @param postCode
    * @return
    */
  def validatePostCode(postCode: String)(using
      ac: Resource[IO, Client[IO]],
      logger: Logger[IO]
  ): IO[PostCodeValidate] = {

    for {
      _ <- logger.info("Sending postCode for validation")
      _ <- IO(println("validating"))
      uri <- IO.fromEither(
        Uri.fromString(s"https://api.postcodes.io/postcodes/$postCode")
      )

      resp <- ac.use { client =>
        client.expect[Json](uri)
      }
      _ <- logger.info("Postcode for validated")
      _ <- IO(println(resp.toString))
      result <- IO.fromEither(addressDecoder(resp.hcursor))
    } yield result
  }

  def addCustomer(user: Customer)(using
      Resource[IO, Client[IO]],
      Resource[IO, HikariTransactor[IO]],
      Logger[IO]
  ): IO[Either[ClientErrorMessage, Int]] = {
    val psVal = validatePostCode(user.postCode)
    for {
      a <- psVal
      newClient = createNewClient(
        Customer(
          user.firstName,
          user.lastName,
          user.emailAddress,
          user.firstLineAddress,
          a.region,
          a.postCode,
          user.age,
          user.creditScore
        )
      )
      output <- addNewUser(newClient)
    } yield output

  }

  def fetchCustomer(userId: String)(using
      Resource[IO, HikariTransactor[IO]]
  ): IO[Either[ClientErrorMessage, ExistingClient]] = {
    fetchUser(userId)
  }

}
