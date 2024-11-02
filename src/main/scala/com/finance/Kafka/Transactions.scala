package com.finance.Kafka

import cats.effect.{IO, Resource}
import fs2.kafka.*
import com.finance.Initialise.config
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
      consumerSett: ConsumerSettings[IO, String, String],
                         producerSett: ProducerSettings[IO, String, String],
      logger: Logger[IO],
      db: Resource[IO, BigtableDataClient]
  ): IO[Unit] = {

    val stream =
      KafkaConsumer
        .stream(consumerSett)
        .subscribeTo(transfersTopic)
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processTransfer(committable.record).handleErrorWith{ err =>
              logger.error(s"Failed to process record: ${committable.record.key}. Error: $err")
              sendToDeadLetterQueue(committable.record, s"${transfersTopic}-dlq").as(committable)
            }
          }
        }
        .parJoinUnbounded

    stream.compile.drain
  }

  def consumeWithdrawals()(using
                           consumerSett: ConsumerSettings[IO, String, String],
                           producerSett: ProducerSettings[IO, String, String],
      logger: Logger[IO],
      db: Resource[IO, BigtableDataClient]
  ): IO[Unit] = {

    val stream =
      KafkaConsumer
        .stream(consumerSett)
        .subscribeTo(withdrawalTopic)
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processRecord(committable.record).handleErrorWith{ err =>
              logger.error(s"Failed to process record: ${committable.record.key}. Error: $err")
              sendToDeadLetterQueue(committable.record, s"${withdrawalTopic}-dlq").as(committable)
            }
          }
        }
        .parJoinUnbounded
        .repeat

    stream.compile.drain
  }

  def consumeDeposits()(using
                        consumerSett: ConsumerSettings[IO, String, String],
                        producerSett: ProducerSettings[IO, String, String],
      logger: Logger[IO],
      db: Resource[IO, BigtableDataClient]
  ): IO[Unit] = {

    val stream =
      KafkaConsumer
        .stream(consumerSett)
        .subscribeTo(depositsTopic)
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processRecord(committable.record).handleErrorWith{ err =>
              logger.error(s"Failed to process record: ${committable.record.key}. Error: $err")
              sendToDeadLetterQueue(committable.record, s"${depositsTopic}-dlq").as(committable)
            }
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

  def processTransfer(
                       record: ConsumerRecord[String, String]
                     )(using logger: Logger[IO], db: Resource[IO, BigtableDataClient]): IO[Unit] = {
    val parsedValue: IO[Int] = IO {
      record.value.toInt
    }.handleErrorWith { (_: Throwable) => IO.raiseError(new RuntimeException("Invalid record value"))}
    parsedValue.flatMap { value =>
      writeToTable((record.key, value.toString)).handleErrorWith { err =>
        logger.error(s"Failed to write to table for key: ${record.key}. Error: $err") *>
          IO.raiseError(err)
      }
    }
  }

  def sendToDeadLetterQueue(badRecord: ConsumerRecord[String, String], deadLetterQueue: String)
                           (using producerSett: ProducerSettings[IO, String, String] ): IO[Unit] =
    KafkaProducer[IO].resource(producerSett).use{ producer =>
      val msg = ProducerRecord(deadLetterQueue, badRecord.key, badRecord.value)
      val wrapMsg = ProducerRecords.one(msg)
      producer.produce((wrapMsg)).void
    }
}
