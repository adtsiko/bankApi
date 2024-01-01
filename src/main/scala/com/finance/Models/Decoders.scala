package com.finance.Models

import doobie.Write
import io.circe.{Decoder, HCursor}

object Decoders {
  case class PostCodeValidate(postCode: String, region: String)

  case class Customer(firstName: String, lastName: String, emailAddress: String, firstLineAddress: String,
                      SecondLineAddress: String, postCode: String, Age: Int, creditScore: Int)

  case class User(userId: String, name: String, emailAddress: String, region: String, age: Int, creditScore: Int)
  
  case class ErrorMessage(msg: String) extends RuntimeException 

  implicit val addressDecoder: Decoder[PostCodeValidate] = json =>
      for {
        postCode <- json.downField("result").get[String]("postcode")
        region <- json.downField("result").get[String]("region")
      } yield PostCodeValidate(postCode, region)


}
