package som.storyforge

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import zio.{Task, ZIO}

trait MessageResult {
  def addReactions(emoji: Iterable[Emoji]): Task[Unit]
  def editMessage(newMessage: String): Task[Unit]
}
case class ConsoleMessageResult(m: String) extends MessageResult {
  override def editMessage(newMessage: String): Task[Unit] =
    zio.Console.printLine(s"(수정됨) $newMessage")

  override def addReactions(emojis: Iterable[Emoji]): Task[Unit] = {
    val s =
      emojis.toList.zipWithIndex
        .map { case (x, i) => s"[$i] ${x.getName}" }

    zio.Console.printLine(s.mkString("\n"))
  }
}
case class DiscordMessageResult(m: Message) extends MessageResult {

  def getChannel: MessageChannelUnion = m.getChannel

  override def editMessage(newMessage: String): Task[Unit] =
    ZIO.attempt(getChannel.editMessageById(m.getIdLong, newMessage).queue())

  override def addReactions(emojis: Iterable[Emoji]): Task[Unit] =
    ZIO.foreachDiscard(emojis)(emoji =>
      ZIO.attempt(m.addReaction(emoji).queue())
    )

}
