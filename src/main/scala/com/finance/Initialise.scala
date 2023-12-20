package com.finance

import cats.effect.{IO, Resource}
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import com.typesafe.config.ConfigFactory
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Initialise {

    val config = ConfigFactory.load()
    private val dbHost = config.getString("db.host")
    private val dbName = config.getString("db.db")
    private val dbUsername = config.getString("db.username")
    private val dbPassword = config.getString("db.password")
    private val dbPort = config.getInt("db.port")

    given client: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build

    given transactor: Resource[IO, HikariTransactor[IO]] =
      for {
        ce <- ExecutionContexts.fixedThreadPool[IO](16) // our connect EC
        xa <- HikariTransactor.newHikariTransactor[IO](
          "org.postgresql.Driver",
          s"jdbc:postgresql://$dbHost:$dbPort/$dbName",
          dbUsername,
          dbPassword,
          ce
        )
      } yield xa

    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
}
