package com.finance.validate

import cats.effect.{IO, Resource}
import com.finance.Models.Decoders.PostCodeValidate
import com.finance.Models.UserModels.ClientErrorMessage
import io.circe.Decoder
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import io.circe.parser.decode
object CreateARequest {

  def getJsonRequest[A, E <: Throwable](uri: Uri, catchErr: String => E)(using
      ac: Resource[IO, Client[IO]],
      logger: Logger[IO],
      decoder: Decoder[A]
  ): IO[Either[E, A]] = {
    for {
      resp <- ac.use { client =>
        client.run(Request[IO](Method.GET, uri)).use { response =>
          response.bodyText.compile.string.map { body =>
            if (response.status.isSuccess) {
              decode[A](body).fold(
                err => Left(catchErr(s"Failed to decode JSON: $err")),
                value => Right(value)
              )
            } else {
              println(
                s"Received unsuccessful response: ${response.status.code}"
              )
              Left(
                catchErr(
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
