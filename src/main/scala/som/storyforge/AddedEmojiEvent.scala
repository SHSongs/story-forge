package som.storyforge

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

trait AddedEmojiEvent {

  def result: Emoji

  def compare(emoji: Emoji): Boolean

  def compare(unicode: String): Boolean
}

case class ConsoleAddedEventEmojiEvent(emoji: Emoji) extends AddedEmojiEvent {
  override def result: Emoji = emoji

  override def compare(emoji: Emoji): Boolean =
    result.getAsReactionCode == emoji.getAsReactionCode

  override def compare(unicode: String): Boolean = result.getName == unicode
}

case class DiscordAddedEventEmojiEvent(event: MessageReactionAddEvent)
    extends AddedEmojiEvent {
  def result: Emoji = event.getReaction.getEmoji

  def compare(emoji: Emoji): Boolean =
    result.getAsReactionCode == emoji.getAsReactionCode

  def compare(unicode: String): Boolean = result.getName == unicode
}
