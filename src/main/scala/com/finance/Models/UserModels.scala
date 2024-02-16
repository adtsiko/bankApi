package com.finance.Models

import com.github.f4b6a3.uuid.UuidCreator
import doobie.Write
import doobie.postgres.implicits._
import java.util.UUID

object UserModels {
  given Write[UUID] = Write[String].contramap(_.toString)

  given Write[PrimaryUserInfo] =
    Write[(String, String, String, String, String, Int)].contramap { usr =>
      (
        usr.userId.toString,
        usr.title,
        usr.firstName,
        usr.lastName,
        usr.emailAddress,
        usr.age
      )
    }

  given Write[Occupation] =
    Write[(String, Int, String)].contramap { usr =>
      (
        usr.occupation,
        usr.income,
        usr.industry
      )
    }

  given Write[Address] =
    Write[(String, String, String)].contramap { usr =>
      (
        usr.firstLineAddress,
        usr.region,
        usr.postCode
      )
    }
  case class ExistingClient(
      userId: String,
      title: String,
      firstName: String,
      lastName: String,
      emailAddress: String,
      age: Int
  )

  case class ClientErrorMessage(msg: String) extends Throwable

  case class RegistrationErrorMessage(msg: String) extends Throwable

  case class Address(
      firstLineAddress: String,
      region: String,
      postCode: String
  )

  case class Occupation(
      occupation: String,
      income: Int,
      industry: String
  )
  case class UserRegistrationBody(
      title: String,
      firstName: String,
      lastName: String,
      emailAddress: String,
      phoneNumber: Long,
      address: Address,
      occupation: Occupation,
      maritalStatus: String,
      homeOwner: Boolean,
      age: Int
  )

  case class PrimaryUserInfo(
      userId: UUID,
      title: String,
      firstName: String,
      lastName: String,
      emailAddress: String,
      age: Int
  )

  def createNewClient(
      user: UserRegistrationBody
  ): (PrimaryUserInfo, Occupation, Address) = {
    val userId: UUID = UuidCreator.getNameBasedSha1(user.toString)
    val primaryInfo = PrimaryUserInfo(
      userId,
      user.title,
      user.firstName,
      user.lastName,
      user.emailAddress,
      user.age
    )
    val occupation = user.occupation.copy()
    val address = user.address.copy()
    (primaryInfo, occupation, address)
  }
}
