package som.storyforge

import net.dv8tion.jda.api.entities.emoji.Emoji
import zio._

trait InteractionService {
  def sendDisplayOptionsAndWaitForSelect(
      optionTitle: String,
      options: List[String]
  ): Task[SelectedRowEvent]

  def showReactionsAndWaitForSelectOne(
      targetMessage: MessageResult,
      emojis: Emoji*
  ): Task[AddedEmojiEvent]

  def sendMessage(message: String): Task[MessageResult]

  def modifyMessage(
      message: String,
      messageResult: MessageResult
  ): Task[MessageResult]
}

object InteractionService {
  def sendDisplayOptionsAndWaitForSelect(
      optionTitle: String,
      options: List[String]
  ): ZIO[InteractionService, Throwable, SelectedRowEvent] =
    ZIO.serviceWithZIO(
      _.sendDisplayOptionsAndWaitForSelect(optionTitle, options)
    )

  def showReactionsAndWaitForSelectOne(
      targetMessage: MessageResult,
      emojis: Emoji*
  ): ZIO[InteractionService, Throwable, AddedEmojiEvent] =
    ZIO.serviceWithZIO(
      _.showReactionsAndWaitForSelectOne(targetMessage, emojis: _*)
    )

  def sendMessage(
      message: String
  ): ZIO[InteractionService, Throwable, MessageResult] =
    ZIO.serviceWithZIO(_.sendMessage(message))

  def modifyMessage(
      message: String,
      messageResult: MessageResult
  ): ZIO[InteractionService, Throwable, MessageResult] =
    ZIO.serviceWithZIO(_.modifyMessage(message, messageResult))
}
