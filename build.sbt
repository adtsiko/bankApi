ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "3.3.1"

val http4sVersion = "0.23.24"
val http4sBlazerVersion = "0.23.9"
val doobieVersion = "1.0.0-RC4"
val testContainerVersion = "0.40.12"


lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "com.finance",
        scalaVersion := "3.3.1",
        mainClass in Compile := Some("com.finance.Entrypoint"),
        dockerBaseImage       := "eclipse-temurin:11",
        dockerExposedPorts    := Seq(8080)
      )
    ),
    name := "bank-api",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.9.4",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.9.4",
      "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.2.10",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "1.2.10",
      "org.typelevel" %% "cats-effect" % "3.5.2",
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sBlazerVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sBlazerVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-h2" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC4",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC4",
      "com.typesafe" % "config" % "1.4.2",
      "com.github.f4b6a3" % "uuid-creator" % "5.3.3",
      "org.typelevel" %% "log4cats-core" % "2.5.0",
      "org.typelevel" %% "log4cats-slf4j" % "2.5.0",
      "org.slf4j" % "slf4j-api" % "2.0.5",  // SLF4J API
      "ch.qos.logback" % "logback-classic" % "1.4.12",
      "org.scalatest" %% "scalatest" % "3.2.15",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
      "com.dimafeng" %% "testcontainers-scala" % testContainerVersion,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainerVersion,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % testContainerVersion % "test",
      "com.dimafeng" %% "testcontainers-scala-mysql" % testContainerVersion % "test",
      "org.postgresql" % "postgresql" % "42.5.4",
      "com.google.cloud.bigtable" % "bigtable-client-core" % "1.28.0",
      "com.google.cloud" % "google-cloud-bigtable-emulator" % "0.167.0"

    ),

  )
enablePlugins(JavaAppPackaging, DockerPlugin)
