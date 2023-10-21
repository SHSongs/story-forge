package som.storyforge

import izumi.reflect.Tag
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.{OptionMapping, OptionType}
import net.dv8tion.jda.api.interactions.commands.build.{
  Commands,
  OptionData,
  SlashCommandData
}
import som.storyforge.StoryForgeDiscord.InputString
import zio.{Chunk, RIO}

import scala.jdk.CollectionConverters.SeqHasAsJava
case class DiscordSlashCommand(
    name: String,
    description: String,
    options: List[SlashCommandOption[_]] = List.empty
) {
  def buildData: SlashCommandData => SlashCommandData = identity

  def toData: SlashCommandData =
    buildData(
      Commands.slash(name, description).addOptions(options.map(_.toData).asJava)
    )
}
class SlashCommands[-Env: Tag](a: Chunk[SlashCommandHandler[Env]]) {
  def get(name: String): SlashCommandHandler[Env] = a
    .find(_.discordSlashCommand.name == name)
    .getOrElse(throw new Exception(s"command $name not found"))
  def discordCommand: Chunk[DiscordSlashCommand] = a.map(_.discordSlashCommand)
}

object SlashCommands {
  def apply[Env: izumi.reflect.Tag](
      a: Chunk[SlashCommandHandler[Env]]
  ): SlashCommands[Env] =
    new SlashCommands(
      a
    )
}

abstract class SlashCommandOptionType[A <: Any](
    val baseOptionType: OptionType
) {
  def fromMap(mapping: OptionMapping): Option[A]
}

abstract class SlashCommandOption[A](
    val optionType: SlashCommandOptionType[A]
) {
  val name: String
  val description: String
  val isRequired: Boolean = false
  val isAutoComplete: Boolean = false
  val choices: List[Choice] = List.empty

  def fromEvent(implicit e: SlashCommandInteractionEvent): Option[A] =
    Option(e.getOption(name)).flatMap(optionType.fromMap)

  def buildData: OptionData => OptionData = identity

  def toData: OptionData =
    buildData(
      new OptionData(
        optionType.baseOptionType,
        name,
        description,
        isRequired,
        isAutoComplete
      ).addChoices(choices.asJava)
    )

}

trait SlashCommandHandler[-Env: Tag] {
  val discordSlashCommand: DiscordSlashCommand

  def task(
      in: SlashCommandInteractionEvent,
      interactionService: DiscordInteractionService
  ): RIO[Env, Unit]
}
