package com.finance.Query

import cats.effect.kernel.Outcome.{Errored, Succeeded}
import cats.effect.{IO, Resource}
import com.finance.Models.Decoders.addressDecoder
import org.typelevel.log4cats.Logger
import com.github.f4b6a3.uuid.UuidCreator
import com.finance.Models.UserModels.*
import doobie._
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.implicits.*
import cats.implicits.*
import doobie.postgres.*
import java.util.UUID
import scala.io.Source

object postgres {
  // TODO Incase of failure delete all inserts

  def insertRegistrationQuery(
      user: PrimaryUserInfo,
      address: Address,
      occ: Occupation
  )(using
      db: Resource[IO, HikariTransactor[IO]],
      logger: Logger[IO]
  ): IO[Either[RegistrationErrorMessage, String]] = {

    def insertIntoUsers() = {
      Update[PrimaryUserInfo](""" 
       INSERT INTO users (userid, title, firstname, lastname, emailaddress, age ) VALUES (?, ?, ?, ?, ?, ?);
      """).run(user)
    }

    def insertIntoAddress() = {
      Update[(String, String, String, String)](s"""
       INSERT INTO users_address (userid, firstlineaddress, region, postcode) VALUES (?, ?, ?, ?);
      """).run(user.userId.toString, address.firstLineAddress, address.region, address.region)
    }
    def insertIntoOccupation() = {
      Update[(String, String, String, Int)]("""
       INSERT INTO users_occupation (userid, occupation, industry, income) VALUES(?, ?, ?, ?)
      """).run(user.userId.toString, occ.occupation, occ.industry, occ.income)
    }

    def deleteInserts() = {
      sql"""
      DELETE FROM users WHERE userid = ${user.userId.toString};
      DELETE FROM users_address WHERE userid = ${user.userId.toString};
      DELETE FROM users_occupation WHERE userid = ${user.userId.toString};
      """
    }
    println(address.toString())
    println(insertIntoAddress())
    val query = for {
      maybeInsertedUser <- insertIntoUsers()
      maybeInsertedAddress <- insertIntoAddress()
      maybeInsertOccupation <- insertIntoOccupation()
    } yield maybeInsertOccupation

    db.use { xa =>
      query.transact(xa).attempt.flatMap {
        case Right(_) => IO(Right(user.userId.toString()))
        case Left(e) =>
          for {
            _ <- logger.info("Failed to insert users")
            query <- IO(deleteInserts())
            _ <- query.update.run.transact(xa)
          } yield Left(RegistrationErrorMessage(s"Failed to Insert User: '${user.userId}'"))
      }
    }
  }

  def fetchUser(userId: String)(using
      xa: Resource[IO, HikariTransactor[IO]]
  ): IO[Either[ClientErrorMessage, ExistingClient]] = {
    xa.use { client =>
      for {
        query <- IO(
          sql"SELECT * FROM users where userid=$userId"
            .query[ExistingClient]
            .option
        )
        userOutput <- query.transact(client)

        b = userOutput.toRight(ClientErrorMessage(s"User unknown"))

      } yield b

    }
  }
}
