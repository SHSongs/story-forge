import net.dv8tion.jda.api.entities.emoji.Emoji
import som.storyforge.{ConsoleInteractionService, InteractionService}
import zio._
import zio.test._

object StoryForgeDiscordSpec extends ZIOSpecDefault {

  val prog = for {
    interaction <- ZIO.service[InteractionService]
    res <- interaction.sendMessage("hello?")
  } yield res
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("DiscordSpec ")(
      test("send message") {
        for {
          _ <- prog
          vector <- TestConsole.output
        } yield assertTrue(vector(0) == "\nhello?\n")
      },
      test("reaction add and choice") {
        for {
          _ <- TestConsole.feedLines("0")

          message <- InteractionService.sendMessage("hello?")
          emoji = Emoji.fromUnicode("✅")
          addReactionEvent <- InteractionService
            .showReactionsAndWaitForSelectOne(
              message,
              emoji
            )

        } yield assertTrue(addReactionEvent.compare(emoji))
      },
      test("select action row and choice") {
        for {
          _ <- TestConsole.feedLines("0")

          selectedEvent <- InteractionService
            .sendDisplayOptionsAndWaitForSelect(
              optionTitle = s"암살에 사용할 무기를 고르시오",
              options = List("맨손", "작은 검")
            )

        } yield assertTrue(selectedEvent.getSelectedNumber == 0)
      },
      test("modify") {
        for {
          message <- InteractionService.sendMessage("hello?")

          newMessage <- InteractionService.modifyMessage("hi!", message)
          vector <- TestConsole.output

        } yield assertTrue(
          vector(0) == "\nhello?\n" && vector(1) == "(수정됨) hi!\n"
        )
      }
    ).provideSome[Scope](
      ConsoleInteractionService.layer
    )
}
