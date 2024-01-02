package com.finance.Models

import com.github.f4b6a3.uuid.UuidCreator
import doobie.Write
import doobie.implicits.javasql._
import java.util.UUID

object UserEntities {
  given Write[UUID] = Write[String].contramap(_.toString)

  given Write[NewClient] = Write[(UUID, String, String, String, Int, Int)].contramap { usr =>
    (usr.userId, usr.name, usr.emailAddress, usr.region, usr.age, usr.creditScore)
  }
  case class ExistingClient(userId: String, name: String, emailAddress: String, region: String, age: Int, creditScore: Int)

  case class ClientErrorMessage(msg: String) extends Throwable
  
  case class Customer(
                       firstName: String,
                       lastName: String,
                       emailAddress: String,
                       firstLineAddress: String,
                       secondLineAddress: String,
                       postCode: String,
                       age: Int,
                       creditScore: Int
                     )

  case class NewClient(
                        userId: UUID,
                        name: String,
                        emailAddress: String,
                        region: String,
                        age: Int,
                        creditScore: Int
                      )

  def createNewClient(user: Customer): NewClient = {
    val name: String = s"${user.firstName} ${user.lastName}"
    val userId: UUID = UuidCreator.getNameBasedSha1(
      s"${user.firstName}${user.lastName}${user.emailAddress}${user.firstLineAddress}${user.secondLineAddress}"
    )
    NewClient(
      userId = userId,
      name = name,
      emailAddress = user.emailAddress,
      region = user.secondLineAddress,
      age = user.age,
      creditScore = user.creditScore
    )
  }
}
