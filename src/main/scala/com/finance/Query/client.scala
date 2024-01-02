package com.finance.Query

import cats.effect.kernel.Outcome.{Errored, Succeeded}
import cats.effect.{IO, Resource}
import com.finance.Models.Decoders.addressDecoder
import com.finance.Models.UserEntities.{ClientErrorMessage, ExistingClient, NewClient}
import com.github.f4b6a3.uuid.UuidCreator
import doobie.Update
import doobie.hikari.HikariTransactor
import doobie.implicits.*

import java.util.UUID


object client {

  def addNewUser(user: NewClient)(using db: Resource[IO, HikariTransactor[IO]]): IO[Either[ClientErrorMessage, Int]] = {
      db.use{
        xa => for{
        query <- IO("INSERT INTO users (userid, name, emailaddress, region, age, creditscore) VALUES(?, ?, ?, ?, ?, ?)")
        b <- Update[NewClient](query).run(user).transact(xa).attempt
        commitUserStatus = b.left.map(e => ClientErrorMessage(s"Failed: $e"))

      } yield commitUserStatus

    }
  }

  def fetchUser(userId: String)(using xa: Resource[IO, HikariTransactor[IO]]): IO[Either[ClientErrorMessage, ExistingClient]]= {
    xa.use {
      client =>
        for {
          query <- IO(sql"SELECT * FROM users where userid=$userId".query[ExistingClient].option)
          userOutput <- query.transact(client)

          b = userOutput.toRight(ClientErrorMessage(s"User unknown"))

        } yield b

    }
  }
}
