name := "doobie-migrations-postgres"
description :=
  """
    |Schema migrations built using doobie for Postgresql
  """.stripMargin
version := "0.1"

scalaVersion := "2.11.12"
sbtVersion := "1.1.1"

val typesafeConfig = "com.typesafe" % "config" % "1.3.2"

val doobieVersion = "0.5.1"
val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion

libraryDependencies ++= Seq(
  doobieCore,
  doobiePostgres
)

val loggerLibs = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3"
)

libraryDependencies ++= loggerLibs

val testLibs = Seq(
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
)

libraryDependencies ++= testLibs
libraryDependencies += typesafeConfig % "test"

val testDbName = "postgres"
val testDbUser = "postgres"
val testDbPass = "postgres"

dockerRunContainers in Test := Seq(
  "postgres" -> DockerRunContainer(
    image = "postgres:10.5",
    containerPort = Some(5432),
    environment = Seq(
      "POSTGRES_DB" -> testDbName,
      "POSTGRES_USER" -> testDbUser,
      "POSTGRES_PASSWORD" -> testDbPass
    ),
    waitHealthy = false // it would be great to use this but `docker inspect` for the postgres container does not have a State.Health.Status which makes it fail unless we set this to false
  )
)

fork in Test := true // required to use javaOptions

javaOptions in Test := {
  val bindPorts = (dockerRunStart in Test).value
  val result = Seq(
    "postgres.url"  -> s"jdbc:postgresql://localhost:${bindPorts("postgres")}/$testDbName",
    "postgres.user" -> testDbUser,
    "postgres.pass" -> testDbPass
  ).map {
    case (key, value) => s"-D$key=$value"
  }
  streams.value.log.info(s"Using environment: \n  ${result.mkString("\n  ")}")
  result
}

test in Test := (dockerRunSnapshot in Test).dependsOn(test in Test).value,