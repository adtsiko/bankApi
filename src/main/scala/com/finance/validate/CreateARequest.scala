package com.finance.validate

import cats.effect.{IO, Resource}
import com.finance.Models.Decoders.PostCodeValidate
import com.finance.Models.UserEntities.ClientErrorMessage
import io.circe.Decoder
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import io.circe.parser.decode
object CreateARequest {

  def getJsonRequest[A](uri: Uri)(using
      httpClient: Resource[IO, Client[IO]],
      logger: Logger[IO],
      decoder: Decoder[A]
  ): IO[Either[ClientErrorMessage, A]] = {
    for {
      resp <- httpClient.use { client =>
        client.run(Request[IO](Method.GET, uri)).use { response =>
          response.bodyText.compile.string.map { body =>
            if (response.status.isSuccess) {
              println(body)
              decode[A](body).fold(
                err => Left(ClientErrorMessage(s"Failed to decode JSON: $err")),
                value => Right(value)
              )
            } else {
              // Unsuccessful response, return an error
              Left(
                ClientErrorMessage(
                  s"Received unsuccessful response: ${response.status.code}"
                )
              )
            }
          }
        }
      }
    } yield resp
  }

}
