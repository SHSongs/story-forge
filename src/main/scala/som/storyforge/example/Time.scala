package som.storyforge.example

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import som.storyforge.{
  DiscordInteractionService,
  DiscordSlashCommand,
  SlashCommandHandler
}
import som.storyforge.SlashCommandOption
import som.storyforge.StoryForgeDiscord.InputString
import zio._
import net.dv8tion.jda.api.EmbedBuilder

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

object Time extends SlashCommandHandler[TimeRepository] {
  private case class MinutesTimePicker()
      extends SlashCommandOption(
        optionType = InputString
      ) {
    val name = "minutes_time_picker"
    val description = "분 선택"
  }

  private case class HoursTimePicker()
      extends SlashCommandOption(
        optionType = InputString
      ) {
    val name = "hours_time_picker"
    val description = "시간 선택"
  }

  override val discordSlashCommand: DiscordSlashCommand =
    DiscordSlashCommand(
      "time",
      "시간 유틸리티 (KST)",
      options = List(HoursTimePicker(), MinutesTimePicker())
    )

  override def task(
      in: SlashCommandInteractionEvent,
      interactionService: DiscordInteractionService
  ) = {
    for {
      _ <- ZIO.unit
      hoursOption = Try(in.getOption("hours_time_picker").getAsInt)
        .getOrElse(0)
      minutesOption = Try(in.getOption("minutes_time_picker").getAsInt)
        .getOrElse(0)

      formatter = DateTimeFormatter.ofPattern("a hh:mm")

      currentTime <- Calendar.nowInSeoul

      addTime = currentTime.plusHours(hoursOption).plusMinutes(minutesOption)

      report =
        s"""${currentTime.format(formatter)}의
           |${hoursOption}시간 ${minutesOption}분 뒤는
           |${addTime.format(formatter)}입니다.
           |""".stripMargin

      embedBuilder = new EmbedBuilder()
        .setTitle(addTime.format(formatter))
        .setDescription(report)
        .setColor(0xff0000)
        .build()

      _ <- ZIO.attempt(in.replyEmbeds(embedBuilder).queue())

      _ <- ZIO.serviceWithZIO[TimeRepository](_.record(LocalDate.now()))
    } yield ()
  }

}
