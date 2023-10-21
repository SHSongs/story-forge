package som.storyforge

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import som.storyforge.StoryForgeDiscord.ReplyCallbackEvent
import zio.{Task, ZIO}

trait SelectedRowEvent {
  def getSelectedNumber: Int

  def getSelectedValue: String
}
case class ConsoleSelectedRowEvent(num: Int) extends SelectedRowEvent {
  override def getSelectedNumber: Int = num

  override def getSelectedValue: String = num.toString
}

case class DiscordSelectedRowEvent(event: StringSelectInteractionEvent)
    extends SelectedRowEvent
    with ReplyCallbackEvent {
  override def getSelectedNumber: Int = event.getValues.get(0).toInt

  override def getSelectedValue: String = event.getValues.get(0)

  override def reply(message: String): Task[Unit] =
    ZIO.attempt(event.reply(message).queue())
}
