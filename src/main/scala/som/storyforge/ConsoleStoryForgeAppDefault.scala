package som.storyforge

import som.storyforge.AppConfig.configProvider
import zio._

object ConsoleStoryForgeAppDefault {

  val consolePreparedLayer =
    ConsoleInteractionService.layer ++
      (
        ZLayer(
          ZIO.config(LoggingConfig.config)
        ) >>> Logging.loggingLayer
      )

}
