package com.finance.Kafka

import cats.effect.{IO, Resource}
import fs2.kafka.*
import com.finance.Initialise.consumerSettings
import com.finance.Query.bigTable.writeToTable
import com.google.cloud.bigtable.data.v2.BigtableDataClient
import fs2.Stream
import org.typelevel.log4cats.Logger
object Transactions {

  def processRecord(record: ConsumerRecord[String, String])(using logger: Logger[IO], db: Resource[IO, BigtableDataClient]): IO[Unit] =
    val message = Option(record.value).getOrElse("null")
    logger.info(s"Processing record: $message")
    writeToTable((record.key, record.value))

  def consumeTransactions()(using kafka: Resource[IO, KafkaConsumer[IO, String, String]], logger: Logger[IO], db: Resource[IO, BigtableDataClient] ): IO[Unit] = {

    val stream =
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo("transactions")
        .partitionedRecords
        .map { partitionStream =>
          partitionStream.evalMap { committable =>
            processRecord(committable.record)
          }
        }
        .parJoinUnbounded.repeat

    stream.compile.drain
  }
}
