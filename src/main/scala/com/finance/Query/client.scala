package com.finance.Query

import cats.effect.kernel.Outcome.{Errored, Succeeded}
import cats.effect.{IO, Resource}
import com.finance.Models.Decoders.{Customer, ErrorMessage, User}
import com.github.f4b6a3.uuid.UuidCreator
import doobie.Update
import doobie.hikari.HikariTransactor
import doobie.implicits.*

import java.util.UUID


object client {
  /**
   * @param user
   * @param xa
   * @return
   */
  def addNewUser(user: Customer)(using xa: Resource[IO, HikariTransactor[IO]]): IO[Either[ErrorMessage, Int]] = {
    val userId: UUID = UuidCreator.getNameBasedSha1(s"${user.firstName}${user.lastName}${user.emailAddress}${user.firstLineAddress}${user.SecondLineAddress}")
    xa.use{
      client => for{
        query <- IO(s"INSERT INTO users (userid, name, emailaddress, region, age, creditscore) VALUES(${userId} ? ? ? ? ?)")
        b <- Update[Customer](query).run(user).transact(client).attemptSomeSqlState( _ => ErrorMessage("User unknown"))
      } yield b

    }
  }

  def fetchUser(userId: String)(using xa: Resource[IO, HikariTransactor[IO]]): IO[Either[ErrorMessage, User]]= {
    xa.use {
      client =>
        for {
          query <- IO(sql"SELECT * FROM users where userid=$userId".query[User].option)
          userOutput <- query.transact(client)

          b = userOutput match {
            case Some(a) => Right(a)
            case _ => Left(ErrorMessage("User unknown"))
          }
        } yield b

    }
  }
}
