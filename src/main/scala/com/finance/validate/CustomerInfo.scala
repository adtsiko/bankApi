package com.finance.validate

import cats.effect.*
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
import com.finance.Models.UserModels.{
  ClientErrorMessage,
  UserRegistrationBody,
  RegistrationErrorMessage,
  ExistingClient,
  createNewClient
}
import com.finance.Query.postgres.{insertRegistrationQuery, fetchUser}
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

  def addCustomer(user: UserRegistrationBody)(using
      Resource[IO, Client[IO]],
      Resource[IO, HikariTransactor[IO]],
      Logger[IO]
  ): IO[Either[RegistrationErrorMessage, String]] = {

    for {
      psVal <- validatePostCode(user.address.postCode)
      _ <- IO(println("I thought you worked"))
      postcodeAndRegion <- IO.fromEither(psVal)
      newClient = createNewClient(
        user.copy(address =
          user.address.copy(
            postCode = postcodeAndRegion.postCode,
            region = postcodeAndRegion.region
          )
        )
      )

      output <- insertRegistrationQuery(
        newClient._1,
        newClient._3,
        newClient._2
      )
    } yield output
  }

  def fetchCustomer(userId: String)(using
      Resource[IO, HikariTransactor[IO]]
  ): IO[Either[ClientErrorMessage, ExistingClient]] = {
    fetchUser(userId)
  }

}
