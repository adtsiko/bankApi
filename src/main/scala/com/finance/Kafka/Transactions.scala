package com.finance.Kafka

import cats.effect.{IO, Resource}
import fs2.kafka.*
import com.finance.Initialise.{config, consumerSettings}
import com.finance.Query.bigTable.writeToTable
import com.google.cloud.bigtable.data.v2.BigtableDataClient
import fs2.Stream
import org.typelevel.log4cats.Logger
object Transactions {
//  transfers
  private val transfersTopic: String =
    config.getString("kafka.topic.transfersTopic")
  val transfersTable: String = config.getString("gcp.bgTable.transfersTable")
//  deposits
  private val depositsTopic: String =
    config.getString("kafka.topic.depositsTopic")
  val depositsTable: String = config.getString("gcp.bgTable.depositsTable")
//  withdrawals
  private val withdrawalTopic: String =
    config.getString("kafka.topic.withdrawalsTopic")
  val withdrawalTable: String = config.getString("gcp.bgTable.withdrawalsTable")
  
  def consumeTransfers()(using
      kafka: Resource[IO, KafkaConsumer[IO, String, String]],
      logger: Logger[IO],
      db: Resource[IO, BigtableDataClient]
  ): IO[Unit] = {

    val stream =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(transfersTopic)
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processRecord(committable.record)
          }
        }
        .parJoinUnbounded
        .repeat

    stream.compile.drain
  }

  def consumeWithdrawals()(using
      kafka: Resource[IO, KafkaConsumer[IO, String, String]],
      logger: Logger[IO],
      db: Resource[IO, BigtableDataClient]
  ): IO[Unit] = {

    val stream =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(withdrawalTopic)
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processRecord(committable.record)
          }
        }
        .parJoinUnbounded
        .repeat

    stream.compile.drain
  }

  def consumeDeposits()(using
      kafka: Resource[IO, KafkaConsumer[IO, String, String]],
      logger: Logger[IO],
      db: Resource[IO, BigtableDataClient]
  ): IO[Unit] = {

    val stream =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(depositsTopic)
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processRecord(committable.record)
          }
        }
        .parJoinUnbounded
        .repeat

    stream.compile.drain
  }

  def processRecord(
      record: ConsumerRecord[String, String]
  )(using logger: Logger[IO], db: Resource[IO, BigtableDataClient]): IO[Unit] =
    val message = Option(record.value).getOrElse("null")
    logger.info(s"Processing record: $message")
    writeToTable((record.key, record.value))

}
