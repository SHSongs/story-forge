package som.storyforge.example

import izumi.reflect.Tag.*
import som.storyforge.*
import som.storyforge.AppConfig.configProvider
import som.storyforge.ConsoleStoryForgeAppDefault.consolePreparedLayer
import zio.*

import java.time.{ZoneId, ZonedDateTime}

object Calendar {
  val tzOfSeoul = ZoneId.of("Asia/Seoul")
  def nowInSeoul: UIO[ZonedDateTime] =
    Clock.currentDateTime.map(_.atZoneSameInstant(tzOfSeoul))
}

object ConsoleApp extends ZIOAppDefault {
  override val bootstrap =
    Runtime.setConfigProvider(configProvider)
  val p = for {
    interactionService <- ZIO.service[InteractionService]
    ref <- Ref.make(0)
    _ <- SampleGame(interactionService).loadGame(ref).ignore
  } yield ()
  override def run = p
    .provideSome[ZIOAppArgs](
      consolePreparedLayer
    )
}

object DiscordApp extends ZIOAppDefault {
  private val slashCommands = SlashCommands.apply(
    Chunk(
      Time,
      SampleGameCommandHandler
    )
  )

  override val bootstrap =
    Runtime.setConfigProvider(configProvider)

  override def run =
    DiscordClient
      .serve(slashCommands)
      .provideSome[ZIOAppArgs](
        Logging.loggingLayer,
        DiscordAPIConfig.layer,
        DiscordInteractionService.layer(None),
        ZLayer(Ref.make("test R type")),
        FakeTimeRepository.layer
      )

}
