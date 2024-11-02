package IntegrationTest

import com.dimafeng.testcontainers.{
  DockerComposeContainer,
  ExposedService,
  ForAllTestContainer,
  KafkaContainer,
  MultipleContainers,
  WaitingForService
}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import fs2.kafka.{
  AutoOffsetReset,
  ConsumerSettings,
  KafkaConsumer,
  KafkaProducer,
  ProducerRecord,
  ProducerRecords,
  ProducerSettings
}
import cats.effect.{IO, Resource}
import com.finance.Initialise.{bgTableInstanceId, gcpProjectId}
import com.dimafeng.testcontainers.DockerComposeContainer.ComposeFile
import com.finance.Kafka.Transactions.consumeTransfers
import com.google.cloud.bigtable.admin.v2.{
  BigtableTableAdminClient,
  BigtableTableAdminSettings
}
import com.google.cloud.bigtable.data.v2.{
  BigtableDataClient,
  BigtableDataSettings
}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import concurrent.duration.DurationInt
import java.io.File
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.testcontainers.containers.wait.strategy.Wait
import java.util.concurrent.TimeoutException
class Ingestion
    extends AnyFlatSpec
    with ForAllTestContainer
    with Matchers
    with BeforeAndAfterAll {

  override val container: DockerComposeContainer = DockerComposeContainer(
    Seq( new File(getClass.getClassLoader.getResource("docker-compose.yaml").getPath)),
    waitingFor = Some(
      WaitingForService(
        "kafka1",
        Wait.forLogMessage(".*[Kafka Server is running.]\n", 1)
      )
    ),
    exposedServices = Seq(
      ExposedService(
        "kafka1",
        29092,
        Wait.forLogMessage(".*[Kafka Server is running.]\n", 1)
      ),
      ExposedService(
        "kafka2",
        29093,
        Wait.forLogMessage(".*[Kafka Server is running.]\n", 1)
      ),
      ExposedService(
        "kafka3",
        29094,
        Wait.forLogMessage(".*[Kafka Server is running.]\n", 1)
      )
    )
  )

  val bootStrapStrapServers = "localhost:9092,localhost:9093,localhost:9094"

  "sendBadRecord" should "send a bad record to the kafka topic" in {
    val producerSettings = ProducerSettings[IO, String, String]
      .withBootstrapServers(bootStrapStrapServers)

    val consumerSettings: ConsumerSettings[IO, String, String] =
      ConsumerSettings[IO, String, String]
        .withBootstrapServers(bootStrapStrapServers)
        .withGroupId("test-group")
        .withAutoOffsetReset(AutoOffsetReset.Earliest)

    val producerResource: Resource[IO, KafkaProducer[IO, String, String]] =
      KafkaProducer[IO].resource(producerSettings)

    val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
    val consumerRes: Resource[IO, KafkaConsumer[IO, String, String]] =
      KafkaConsumer[IO].resource(consumerSettings)

    val bigTableSettings = BigtableDataSettings
      .newBuilder()
      .setProjectId(gcpProjectId)
      .setInstanceId(bgTableInstanceId)
      .build()

    val adminBigTableSettings = BigtableTableAdminSettings
      .newBuilder()
      .setProjectId(gcpProjectId)
      .setInstanceId(bgTableInstanceId)
      .build()

    given Resource[IO, BigtableDataClient] = Resource.make {
      try {
        IO(BigtableDataClient.create(bigTableSettings))
      } catch {
        case e: Exception =>
          IO.raiseError(e)
      }
    } { session =>
      IO(session.close())
    }
    given Resource[IO, BigtableTableAdminClient] = Resource.make {
      IO(BigtableTableAdminClient.create(adminBigTableSettings))
    } { session =>
      IO(session.close())
    }

    val badRecord = ProducerRecord("transfers", "account-1000", "badrecord")
    val wrapRecord = ProducerRecords.one(badRecord)
    producerResource
      .use { producer =>
        for {
          _ <- producer.produce(wrapRecord)
          _ <- consumeTransfers()(using
            consumerSettings,
            producerSettings,
            logger
          ).timeout(15.seconds).handleErrorWith { case _: TimeoutException =>
            logger.error("Consuming transfers timed out.").as(IO.unit)
          }
        } yield ()
      }
      .unsafeRunSync()
  }
}
