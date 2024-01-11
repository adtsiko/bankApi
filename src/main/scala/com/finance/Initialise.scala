package com.finance

import cats.effect.{IO, Resource}
import doobie.{ExecutionContexts, Fragment}
import doobie.implicits.*

import scala.io.Source
import doobie.hikari.HikariTransactor
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import com.typesafe.config.ConfigFactory
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.google.cloud.bigtable.data.v2.BigtableDataClient
import com.google.cloud.bigtable.data.v2.BigtableDataSettings
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}


object Initialise {

  val config = ConfigFactory.load()
  private val dbHost = config.getString("db.host")
  private val dbName = config.getString("db.db")
  private val dbUsername = config.getString("db.username")
  private val dbPassword = config.getString("db.password")
  private val dbPort = config.getInt("db.port")

  private val gcpProjectId = config.getString("gcp.projectId")
  private val bgTableInstanceId = config.getString("gcp.bgTable.instanceId")
  private val bigTableSettings = BigtableDataSettings.newBuilder().setProjectId("cbt-test").setInstanceId("finance-gcp-table").build()
  private val adminBigTableSettings = BigtableTableAdminSettings.newBuilder().setProjectId("cbt-test").setInstanceId("finance-gcp-table").build()

  private val bootStrapStrapServers = config.getString("kafka.bootStrapServers")
  private val logger = Slf4jLogger.getLogger[IO]
  val consumerSettings:  ConsumerSettings[IO, String, String]
  = ConsumerSettings[IO, String, String].withAutoOffsetReset(AutoOffsetReset.Earliest)
    .withBootstrapServers(bootStrapStrapServers).withGroupId("bank-app")

  given Logger[IO] = Slf4jLogger.getLogger[IO]
  given  Resource[IO, KafkaConsumer[IO, String, String]] = KafkaConsumer[IO].resource(consumerSettings)
  given Resource[IO, BigtableDataClient] = Resource.make{
    try {
      logger.info("Creating BigTableClient") >>
        IO(BigtableDataClient.create(bigTableSettings))
    }catch{
      case e: Exception => logger.error(e)("Error")
        IO.raiseError(e)
    }
  }{ session =>
    logger.info("Closing BigTableClient") >>
    IO(session.close())

  }
  given Resource[IO, BigtableTableAdminClient] = Resource.make {
    IO(BigtableTableAdminClient.create(adminBigTableSettings))
  } { session =>
    IO(session.close())

  }


  given client: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build

  given transactor: Resource[IO, HikariTransactor[IO]]  =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](16)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        s"jdbc:postgresql://$dbHost:$dbPort/$dbName",
        dbUsername,
        dbPassword,
        ce
      )
    } yield xa

  def initialiseDb()(using pgDB: Resource[IO, HikariTransactor[IO]], bTDB: Resource[IO, BigtableTableAdminClient], logger: Logger[IO]): IO[Unit] = {

    pgDB.use { xa =>
      for {
        _ <- logger.info("Initialising DB......")
        sqlFileContent <- IO(
          Source.fromResource("sql/initialisation.sql").mkString
        )
        sqlString = Fragment.const(sqlFileContent).update
        _ <- sqlString.run.transact(xa)
        _ <- logger.info("DB Initialisation complete...")
      } yield ()
    } >> bTDB.use{ xa =>
      for {
        _ <- logger.info("Initialising BigTable......")
        table = CreateTableRequest.of("transactions").addFamily("cf1")
        attemptTable <- IO(xa.createTable(table)).attempt
        _  <- attemptTable match {
          case Left(_) => logger.info(s"Table: ${table.toString} already exists")
          case Right(x) => IO(x)
        }
        _ <- logger.info(xa.listTables().toString)

        _ <- logger.info("BigTable Initialisation complete...")
      } yield ()
    }

  }

}