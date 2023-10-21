package som.storyforge

import som.storyforge.AppConfig.configProvider
import zio._

object ConsoleStoryForgeAppDefault extends ZIOAppDefault {

  override val bootstrap =
    Runtime.setConfigProvider(configProvider)
  val consolePreparedLayer =
    ConsoleInteractionService.layer ++
      (
        ZLayer(
          ZIO.config(LoggingConfig.config)
        ) >>> Logging.loggingLayer
      )

  val p = for {
    _ <- ZIO.unit
    interactionService = new ConsoleInteractionService
    a <- zio.Ref.make[Int](0)
  } yield ()
  override def run = p
    .provideSome[ZIOAppArgs](
      consolePreparedLayer
    )

}
