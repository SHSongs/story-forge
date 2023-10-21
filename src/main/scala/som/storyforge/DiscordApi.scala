package som.storyforge

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.{OptionMapping, OptionType}
import net.dv8tion.jda.api.{JDA, JDABuilder}
import zio._
import zio.stream.ZStream
import izumi.reflect.Tag
import scala.jdk.CollectionConverters._

case class DiscordClient[Env: Tag](
    commands: Chunk[DiscordSlashCommand],
    eventProcess: PartialFunction[GenericEvent, ZIO[Env, Throwable, Unit]],
    config: DiscordAPIConfig
) {

  def run: ZIO[
    Env & Scope,
    Throwable,
    (Fiber.Runtime[Nothing, Unit], Queue[GenericEvent], JDA)
  ] =
    ZIO.acquireRelease {
      for {
        runtime <- ZIO.runtime[Any]

        queue <- Queue.unbounded[GenericEvent]
        jda = JDABuilder
          .createDefault(config.token)
          .enableIntents(config.gatewayIntents.asJava)
          .addEventListeners(new EventListener {
            override def onEvent(event: GenericEvent): Unit =
              Unsafe.unsafe(implicit unsafe =>
                runtime.unsafe.runToFuture(queue.offer(event))
              )
          })
          .build()

        fiber <- ZStream
          .fromQueue(queue)
          .mapZIOParUnordered(
            java.lang.Runtime.getRuntime.availableProcessors
          ) { event =>
            ZIO.logInfo(event.toString) *>
              eventProcess
                .applyOrElse(event, (_: GenericEvent) => ZIO.unit)
                .tapErrorCause(error => ZIO.logErrorCause(error).ignore)
                .catchAll(error => ZIO.logError(error.getMessage).ignore)
                .catchAllDefect(error =>
                  ZIO.logErrorCause("fatal error", Cause.fail(error)).ignore
                )
                .unit
          }
          .runDrain
          .forkScoped
          .onInterrupt(ZIO.logInfo("interrupted discord service"))
          .interruptible

        _ <- ZIO
          .async[Any, Nothing, Unit](callback =>
            jda
              .updateCommands()
              .addCommands(
                commands
                  .map(_.toData)
                  .asJava
              )
              .queue(_ => callback(ZIO.unit))
          )
      } yield (fiber, queue, jda)
    } { case (fiber, queue, jda) =>
      (for {
        _ <- ZIO.attempt(jda.shutdownNow())
        _ <- queue.shutdown
        _ <- fiber.interrupt
      } yield ()).either
    }
}

object DiscordClient {

  def install[Env: Tag](
      routes: SlashCommands[Env]
  ): ZIO[
    Env & DiscordInteractionService & DiscordAPIConfig & Scope,
    Throwable,
    Unit
  ] =
    ZIO
      .serviceWith[DiscordAPIConfig](config =>
        DiscordClient(
          routes.discordCommand,
          StoryForgeDiscord.client(routes),
          config
        )
      )
      .flatMap(_.run)
      .unit

  def serve[Env: Tag](
      routes: SlashCommands[Env]
  ): ZIO[Env & DiscordInteractionService & DiscordAPIConfig, Throwable, Unit] =
    ZIO.scoped[Env & DiscordInteractionService & DiscordAPIConfig] {
      install(routes) *> ZIO.never
    }

  object Collect {
    def apply[PR](
        p: PartialFunction[GenericEvent, ZIO[PR, Throwable, Unit]]
    ): PartialFunction[GenericEvent, ZIO[PR, Throwable, Unit]] = p
  }
}
