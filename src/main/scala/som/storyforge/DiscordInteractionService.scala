package som.storyforge

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.selections.{
  SelectOption,
  StringSelectMenu
}
import som.storyforge
import som.storyforge.StoryForgeDiscord._
import zio._

import scala.jdk.CollectionConverters.SeqHasAsJava
case class DiscordInteractionService(
    hook: zio.Ref[Option[InteractionHook]],
    reactionEventQueue: ReactionEventQueue,
    actionRowEventQueue: ActionRowEventQueue
) extends InteractionService {

  def send(message: String): Task[MessageResult] =
    for {
      h <- hook.get
      messageResult <- h match {
        case None => ZIO.fail(new Throwable("hook is none"))
        case Some(h) =>
          ZIO
            .async[Any, Nothing, MessageResult](callback =>
              h
                .sendMessage(message)
                .queue(m => callback(ZIO.succeed(DiscordMessageResult(m))))
            )
      }
    } yield messageResult

  def registerHook(event: SlashCommandInteractionEvent, initMessage: String) =
    replyMessage(event, initMessage).flatMap(h => hook.set(Some(h)))
  def getUserId: Task[String] = hook.get
    .map(_.map(_.getInteraction.getUser.getId))
    .someOrFail(new Throwable("not found user id"))
  override def sendMessage(
      message: String
  ): Task[MessageResult] = for {
    res <- message match {
      case x if x.isEmpty =>
        ZIO.fail(new Throwable("message must not be nonEmpty"))
      case x =>
        send(x)
    }
  } yield res

  override def modifyMessage(
      message: String,
      messageResult: MessageResult
  ): Task[MessageResult] =
    messageResult.editMessage(
      message
    ) *> ZIO.succeed(messageResult)

  override def showReactionsAndWaitForSelectOne(
      targetMessage: MessageResult,
      emojis: Emoji*
  ): Task[AddedEmojiEvent] = for {
    userId <- getUserId
    _ <- targetMessage.addReactions(emojis)
    promise <- Promise.make[Nothing, DiscordAddedEventEmojiEvent]
    _ <- registerReactionListener(
      userId,
      promise
    )
    f <- promise.await.fork
    result <- f.join
  } yield result

  def sendActionRow(
      title: String,
      options: List[String]
  ): Task[MessageResult] = for {
    h <- hook.get
    res <- h match {
      case None =>
        ZIO.fail(new Throwable("hook is none"))
      case Some(h) =>
        ZIO
          .async[Any, Nothing, MessageResult](callback =>
            h
              .sendMessage(title)
              .addActionRow(
                StringSelectMenu
                  .create("x")
                  .addOptions(
                    options.zipWithIndex.map { case (msg, i) =>
                      SelectOption.of(msg, i.toString)
                    }.asJava
                  )
                  .build()
              )
              .queue(m => callback(ZIO.succeed(DiscordMessageResult(m))))
          )
    }
  } yield res

  override def sendDisplayOptionsAndWaitForSelect(
      optionTitle: String,
      options: List[String]
  ): Task[DiscordSelectedRowEvent] = {
    optionTitle match {
      case x if x.isEmpty =>
        ZIO.fail(new Throwable("message must not be nonEmpty"))
      case _ =>
        for {
          userId <- getUserId
          r <- sendActionRow(optionTitle, options)
          promise <- Promise.make[Nothing, DiscordSelectedRowEvent]
          _ <- registerActionRowListener(userId, promise)
          f <- promise.await.fork
          result <- f.join
          _ <-
            replyMessage(
              result.event,
              s"${options(result.getSelectedNumber)}을(를) 선택했습니다"
            )
        } yield result
    }
  }

  def selectedActionRow(
      userId: String,
      event: StringSelectInteractionEvent
  ): UIO[Unit] =
    for {
      maybeHook <- actionRowEventQueue.q.get(userId).commit
      _ <- ZIO.foreachDiscard(maybeHook)(h =>
        actionRowEventQueue.q.delete(userId).commit *> h.succeed(
          DiscordSelectedRowEvent(event)
        )
      )
    } yield ()

  def addedEmoji(
      userId: String,
      event: MessageReactionAddEvent
  ): UIO[Unit] = for {
    maybeHook <- reactionEventQueue.q.get(userId).commit
    _ <- ZIO.foreachDiscard(maybeHook)(h =>
      reactionEventQueue.q.delete(userId).commit
        *> h.succeed(DiscordAddedEventEmojiEvent(event))
    )
  } yield ()

  private def registerReactionListener(
      userId: String,
      promise: Promise[Nothing, DiscordAddedEventEmojiEvent]
  ): Task[Unit] =
    for {
      _ <- reactionEventQueue.q.put(userId, promise).commit
    } yield ()

  private def registerActionRowListener(
      userId: String,
      promise: Promise[Nothing, DiscordSelectedRowEvent]
  ): UIO[Unit] = actionRowEventQueue.q.put(userId, promise).commit

  private def replyMessage(
      event: IReplyCallback,
      message: String
  ): Task[InteractionHook] =
    ZIO
      .async[Any, Nothing, InteractionHook](callback =>
        event
          .reply(message)
          .queue(m => callback(ZIO.succeed(m)))
      )
}

object DiscordInteractionService {

  def layer(hook: Option[InteractionHook] = None): URLayer[
    Any,
    DiscordInteractionService
  ] = ZLayer {
    for {
      hook <- Ref.make(hook)
      actionRowRef <-
        stm.TMap
          .empty[String, Promise[Nothing, DiscordSelectedRowEvent]]
          .commit
          .map(ActionRowEventQueue(_))
      reactionEventQueue <-
        stm.TMap
          .empty[String, Promise[Nothing, DiscordAddedEventEmojiEvent]]
          .commit
          .map(ReactionEventQueue(_))
    } yield storyforge.DiscordInteractionService(
      hook,
      reactionEventQueue,
      actionRowRef
    )
  }
}
