package com.finance.Query

import cats.effect.{IO, Resource}
import com.finance.Models.Decoders.{Customer, User}
import com.github.f4b6a3.uuid.UuidCreator
import doobie.hikari.HikariTransactor
import doobie.implicits.*

import java.util.UUID


object client {
  /**
   * CREATE TABLE users (userId VARCHAR(100) UNIQUE NOT NULL, Name VARCHAR(50) NOT NULL, emailAddress VARCHAR(100) NOT NULL, region VARCHAR(50), age INT NOT NULL, creditScore INT NOT NULL)
   * @param user
   * @param xa
   * @return
   */
  def addNewUser(user: Customer)(using xa: Resource[IO, HikariTransactor[IO]]): IO[String]= {
    val userId: UUID = UuidCreator.getNameBasedSha1(s"${user.firstName}${user.lastName}${user.emailAddress}${user.firstLineAddress}${user.SecondLineAddress}")
    xa.use{
      client => for{
        query <- IO(sql"INSERT INTO users (userid, name, emailaddress, region, age, creditscore) VALUES (${userId.toString}, ${user.firstName}, ${user.emailAddress}, ${user.SecondLineAddress}, ${user.Age}, ${user.creditScore})")
        a <- query.update.run.transact(client)
      } yield userId.toString

    }
  }

  def fetchUser(userId: String)(using xa: Resource[IO, HikariTransactor[IO]]): IO[User]= {
    xa.use {
      client =>
        for {
          query <- IO(sql"SELECT * FROM users where userid=$userId".query[User])
          a <- query.unique.transact(client).debug()
        } yield a

    }
  }
}
