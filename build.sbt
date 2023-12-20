ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "3.3.1"

val http4sVersion = "0.23.24"
val http4sBlazerVersion = "0.23.9"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(List(
    organization    := "com.finance",
    scalaVersion    := "3.3.1"
  )),
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
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sBlazerVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sBlazerVersion,
  "org.http4s" %% "http4s-circe"        % http4sVersion,
  "org.tpolecat" %% "doobie-core"      % "1.0.0-RC4",
  "org.tpolecat" %% "doobie-h2"        % "1.0.0-RC4",          // H2 driver 1.4.200 + type mappings.
  "org.tpolecat" %% "doobie-hikari"    % "1.0.0-RC4",          // HikariCP transactor.
  "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC4",
  "com.typesafe" %  "config"           % "1.4.2",
  "com.github.f4b6a3" % "uuid-creator" % "5.3.3",
  "org.typelevel" %% "log4cats-core"    % "2.5.0",  // Only if you want to Support Any Backend
  "org.typelevel" %% "log4cats-slf4j"   % "2.5.0",  // Direct Slf4j Support - Recommended

)
)