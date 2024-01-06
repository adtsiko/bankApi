package com.finance.validate

import cats.effect.{IO, *}
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import org.http4s.implicits.*
import io.circe.{Decoder, HCursor, Json}
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import com.finance.Models.Decoders.{PostCodeValidate, addressDecoder}
import com.finance.Models.UserEntities.{ClientErrorMessage, Customer, ExistingClient, createNewClient}
import com.finance.Query.postgres.{addNewUser, fetchUser}
import com.finance.validate.CreateARequest.getJsonRequest
import com.finance.validate.CustomerInfo.validatePostCode
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
  ): IO[Either[ClientErrorMessage, PostCodeValidate]] = {

    for {
      _ <- logger.info("Validating postcode")
      uri <- IO.fromEither(
        Uri.fromString(s"https://api.postcodes.io/postcodes/$postCode")
      )
      resp <- getJsonRequest[PostCodeValidate, ClientErrorMessage](
        uri,
        ClientErrorMessage.apply
      )
    } yield resp
  }

  def addCustomer(user: Customer)(using
                                  Resource[IO, Client[IO]],
                                  Resource[IO, HikariTransactor[IO]],
                                  Logger[IO]
  ): IO[Either[ClientErrorMessage, Int]] = {

    for {
      psVal <- validatePostCode(user.postCode)
      _ <- IO(println("I thought you worked"))
      postcodeAndRegion <- IO.fromEither(psVal)
      _ <- IO(println("not yet"))
      newClient = createNewClient(
        Customer(
          user.firstName,
          user.lastName,
          user.emailAddress,
          user.firstLineAddress,
          postcodeAndRegion.region,
          postcodeAndRegion.postCode,
          user.age,
          user.creditScore
        )
      )
      _ <- IO(println("passed"))
      output <- addNewUser(newClient)
    } yield output
  }


  def fetchCustomer(userId: String)(using
      Resource[IO, HikariTransactor[IO]]
  ): IO[Either[ClientErrorMessage, ExistingClient]] = {
    fetchUser(userId)
  }

}
