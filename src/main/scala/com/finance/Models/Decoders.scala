package com.finance.Models

import io.circe.{Decoder, HCursor}

import java.util.UUID

object Decoders {

  case class PostCodeValidate(postCode: String, region: String)

  implicit val addressDecoder: Decoder[PostCodeValidate] = json =>
    for {
      postCode <- json.downField("result").get[String]("postcode")
      region <- json.downField("result").get[String]("region")
    } yield PostCodeValidate(postCode, region)
}
