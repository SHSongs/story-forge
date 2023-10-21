package som.storyforge

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.interactions.commands.{OptionMapping, OptionType}
import zio._
import izumi.reflect.Tag

object StoryForgeDiscord {
  case object InputString
      extends SlashCommandOptionType[String](OptionType.STRING) {
    def fromMap(mapping: OptionMapping): Option[String] = Some(
      mapping.getAsString
    )
  }

  trait ReplyCallbackEvent {
    def reply(message: String): Task[Unit]
  }

  case class ActionRowEventQueue(
      q: stm.TMap[String, Promise[Nothing, DiscordSelectedRowEvent]]
  )

  case class ReactionEventQueue(
      q: stm.TMap[String, Promise[Nothing, DiscordAddedEventEmojiEvent]]
  )

  def client[Env: Tag](
      routes: SlashCommands[Env]
  ): PartialFunction[GenericEvent, ZIO[
    DiscordInteractionService with Env,
    Throwable,
    Unit
  ]] =
    DiscordClient.Collect {
      case event: SlashCommandInteractionEvent =>
        ZIO.serviceWithZIO[DiscordInteractionService](interactionService =>
          routes.get(event.getName).task(event, interactionService)
        )

      case event: StringSelectInteractionEvent =>
        ZIO
          .when(!event.getUser.isBot) {
            ZIO.serviceWithZIO[DiscordInteractionService](
              _.selectedActionRow(event.getUser.getId, event)
            )
          }
          .unit

      case event: MessageReactionAddEvent =>
        ZIO
          .when(!event.getUser.isBot) {
            ZIO.serviceWithZIO[DiscordInteractionService](
              _.addedEmoji(event.getUser.getId, event)
            )
          }
          .unit
    }

}
