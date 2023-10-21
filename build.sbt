scalaVersion := "3.3.1"

maintainer := "som <solver.som@gmail.com>"

val V = new {
  val zioConfig = "4.0.0-RC13"
  val zio = "2.0.13"
}

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % V.zio,
  "dev.zio" %% "zio-test" % V.zio % Test,
  "dev.zio" %% "zio-test-sbt" % V.zio % Test,
  "dev.zio" %% "zio-test-magnolia" % V.zio % Test,
  "dev.zio" %% "zio-config" % V.zioConfig,
  "dev.zio" %% "zio-config-magnolia" % V.zioConfig,
  "dev.zio" %% "zio-config-typesafe" % V.zioConfig,
  "dev.zio" %% "zio-logging" % "2.1.12",
  "dev.zio" %% "zio-logging-slf4j-bridge" % "2.1.12",
  "dev.zio" %% "zio-prelude" % "1.0.0-RC17",
  "net.dv8tion" % "JDA" % "5.0.0-beta.6"
)

enablePlugins(JavaAppPackaging, JDebPackaging)
