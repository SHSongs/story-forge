package som.storyforge

import net.dv8tion.jda.api.entities.emoji.Emoji
import zio.Schedule.Decision
import zio._

import java.io.IOException
class ConsoleInteractionService extends InteractionService {
  val result: MessageResult = ConsoleMessageResult("")

  implicit class ConsoleRetryOps[-R, E >: IOException, A](z: ZIO[R, E, A]) {
    def retryOrElseForceSelect(
        retryCount: Int,
        retryDescription: String,
        forceSelectDescription: String,
        forceSelectValue: A
    )(implicit ev: CanFail[E], trace: Trace): ZIO[R, IOException, A] =
      z.retryOrElse[R, A, Long, IOException](
        Schedule.recurs(retryCount - 1).onDecision { (i, n, decision) =>
          decision match {
            case Decision.Continue(interval) =>
              zio.Console
                .printLine(
                  s"$retryDescription (remaining: ${retryCount - i})"
                )
                .ignoreLogged
            case Decision.Done => ZIO.unit
          }
        },
        { (x: E, y: Long) =>
          zio.Console
            .printLine(s"$forceSelectDescription")
            .delay(1.seconds) *> ZIO.succeed(
            forceSelectValue
          )
        }
      )
  }

  override def sendMessage(
      message: String
  ): Task[MessageResult] =
    zio.Console.printLine("\n" + message) *> ZIO.succeed(result)

  override def showReactionsAndWaitForSelectOne(
      targetMessage: MessageResult,
      emojis: Emoji*
  ): Task[AddedEmojiEvent] = {
    for {
      _ <- sendMessage(s"\n 리액션 선택")
      _ <- targetMessage.addReactions(emojis)

      selectedEmoji <- zio.Console
        .readLine("숫자 입력: ")
        .flatMap(x => ZIO.attempt(x.toInt))
        .flatMap(i => ZIO.attempt(emojis(i)))
        .retryOrElseForceSelect(
          10,
          s"$targetMessage [0 ~ ${emojis.length - 1}]을 입력하시오",
          s"${emojis(0)}번으로 강제 선택",
          emojis(0)
        )

    } yield ConsoleAddedEventEmojiEvent(selectedEmoji)
  }

  override def modifyMessage(
      message: String,
      messageResult: MessageResult
  ): Task[MessageResult] = {
    messageResult.editMessage(message) *> ZIO.succeed(
      ConsoleMessageResult(message)
    )
  }

  def sendActionRow(
      title: String,
      options: List[String]
  ): Task[MessageResult] = for {
    _ <- zio.Console.printLine(s"\n $title")
    s = options.zipWithIndex.map { case (x, i) => s"[$i] ${x}" }
    _ <- zio.Console.printLine(s.mkString("\n"))
  } yield ConsoleMessageResult(title)

  override def sendDisplayOptionsAndWaitForSelect(
      optionTitle: String,
      options: List[String]
  ): Task[ConsoleSelectedRowEvent] =
    for {
      _ <- sendActionRow(
        optionTitle,
        options
      )
      (num) <- zio.Console
        .readLine("숫자 입력: ")
        .flatMap(n => ZIO.attempt(n.toInt))
        .flatMap {
          case n if n < options.length => ZIO.succeed(n)
          case n                       => ZIO.fail(n)
        }
        .retryOrElseForceSelect(
          retryCount = 10,
          s"$optionTitle [0 ~ ${options.length - 1}]을 입력하시오.",
          s"${options.head}로 강제 선택",
          0
        )

    } yield ConsoleSelectedRowEvent(num)
  def send(message: String): Task[MessageResult] =
    zio.Console.printLine(message) *> ZIO.succeed(
      ConsoleMessageResult(message)
    )

}

object ConsoleInteractionService {

  val layer: URLayer[
    Any,
    InteractionService
  ] = ZLayer.succeed(new ConsoleInteractionService)
}
