package som.storyforge.example

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import som.storyforge.*
import zio.*

case class SampleGame(interactionService: InteractionService) {

  private case class Skill(damage: Int, probability: Int) {
    override def toString: String = s"공격력: ${damage}, 확률: ${probability}"
  }
  
  private val aSkill = Skill(20, 99)
  private val qSkill = Skill(40, 50)
  private val wSkill = Skill(60, 30)
  private val rSkill = Skill(80, 20)

  def loadGame(enemyHp: Ref[Int]): ZIO[Any, Throwable, Unit] = for {
    _ <- ZIO.unit

    message <- send(s"""
         |아군이 모두 죽었습니다. 당신은 마지막 생존자입니다.
         |
         |전세를 뒤집을 뭔가가 필요합니다. 몬스터 죽여서 승리를 쟁취하세요.
         |
         |${EmojiStore.dragonEmoji.getName}: 용 (체력 100)
         |${EmojiStore.bugEmoji.getName}: 딱정벌레 (체력 80)
         |""".stripMargin)

    event <- interactionService.showReactionsAndWaitForSelectOne(
      message,
      EmojiStore.dragonEmoji,
      EmojiStore.bugEmoji
    )

    _ <- event match {
      case r if r.compare(EmojiStore.dragonEmoji) => enemyHp.set(100)
      case r if r.compare(EmojiStore.bugEmoji)    => enemyHp.set(80)
    }

    message <- send(
      s"${event.result.getName}이 나타났습니다. 최후 도망 기회를 드리겠습니다. 도망치시겠습니까?"
    )

    event <- interactionService.showReactionsAndWaitForSelectOne(
      message,
      EmojiStore.runEmoji,
      EmojiStore.knifeEmoji
    )

    _ <- event match {
      case r if r.compare(EmojiStore.runEmoji) => send(s"도망 갑니다.")
      case r if r.compare(EmojiStore.knifeEmoji) => fight(enemyHp)
    }

    result <- enemyHp.get
    _ <- result match {
      case res if res > 0 => send("몬스터 처치에 실패하였습니다.")
      case _              => send("몬스터 처치에 성공하였습니다.")
    }

    _ <- send("접속을 종료합니다.") <* ZIO.sleep(2.seconds)
  } yield ()
  
  private def fight(enemyHp: Ref[Int]) = send("3 번의 공격 기회가 있습니다.") *> ZIO.foreach(List(2, 1, 0)) { cnt =>
    for {
      h <- enemyHp.get
      _ <- ZIO.when(h > 0) {
        for {
          selectedEvent <- interactionService
            .sendDisplayOptionsAndWaitForSelect(
              optionTitle = s"공격에 사용할 스킬을 선택하세요.",
              options = List(
                s"A - 평타 - $aSkill",
                s"Q - 블화살 - $qSkill",
                s"W - 화염 폭풍 - $wSkill",
                s"R - 불꽃 쇄도 - $rSkill"
              )
            )
          
          number = selectedEvent.getSelectedNumber
          randomInt <- Random.nextIntBetween(0, 100)

          _ <- number match {
            case 0 if randomInt < aSkill.probability =>
              enemyHp.update(_ - aSkill.damage) *> send("공격에 성공하였습니다.")
            case 1 if randomInt < qSkill.probability =>
              enemyHp.update(_ - qSkill.damage) *> send("공격에 성공하였습니다.")
            case 2 if randomInt < wSkill.probability =>
              enemyHp.update(_ - wSkill.damage) *> send("공격에 성공하였습니다.")
            case 3 if randomInt < rSkill.probability =>
              enemyHp.update(_ - rSkill.damage) *> send("공격에 성공하였습니다.")
            case _ => send("공격에 실패하였습니다.")
          }
          
          h <- enemyHp.get
          _ <- send(s"남은 체력: ${Math.max(0, h)}")

          _ <- ZIO.when(h > 0) {
            send(s"${cnt} 번의 공격 기회가 있습니다.")
          }

          _ <- ZIO.sleep(2.seconds)
        } yield ()
      }
    } yield ()
  }

  def send(message: String): Task[MessageResult] =
    interactionService.sendMessage(message)
}

case object SampleGameCommandHandler extends SlashCommandHandler[Ref[String]] {

  override val discordSlashCommand: DiscordSlashCommand =
    DiscordSlashCommand("play_sample_game", "sample game 시작")

  override def task(
      in: SlashCommandInteractionEvent,
      interactionService: DiscordInteractionService
  ): Task[Unit] =
    for {
      _ <- interactionService.registerHook(in, discordSlashCommand.description)
      a <- zio.Ref.make[Int](0)
      _ <- SampleGame(interactionService).loadGame(a).ignore
    } yield ()

}
