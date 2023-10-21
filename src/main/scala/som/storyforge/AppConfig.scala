package som.storyforge

import net.dv8tion.jda.api.requests.GatewayIntent
import zio._
import zio.config.magnolia._
import zio.config.typesafe.TypesafeConfigProvider
import zio.logging.{ConsoleLoggerConfig, LogFilter, LogFormat}

case class DiscordAPIConfig(
    token: String,
    gatewayIntents: List[GatewayIntent] = List(
      GatewayIntent.GUILD_MESSAGES,
      GatewayIntent.DIRECT_MESSAGES,
      GatewayIntent.MESSAGE_CONTENT
    )
)

object DiscordAPIConfig {
  val layer: ZLayer[Any, Config.Error, DiscordAPIConfig] = ZLayer(
    ZIO.config(DiscordAPIConfig.config)
  )
  implicit val gatewayIntentDerive: DeriveConfig[GatewayIntent] =
    DeriveConfig[String].map {
      case "GUILD_MESSAGES"  => GatewayIntent.GUILD_MESSAGES
      case "DIRECT_MESSAGES" => GatewayIntent.DIRECT_MESSAGES
      case "MESSAGE_CONTENT" => GatewayIntent.MESSAGE_CONTENT
    }
  val config: Config[DiscordAPIConfig] =
    deriveConfig[DiscordAPIConfig].nested("discord-api")
}
case class LoggingConfig(
    path: String
)
object LoggingConfig {
  val config: Config[LoggingConfig] =
    deriveConfig[LoggingConfig].nested("logging")
}
object Logging {

  val loggingLayer =
    ZLayer(ZIO.config(LoggingConfig.config)) >>>
      ZLayer(ZIO.service[LoggingConfig]).flatMap { conf =>
        zio.logging.removeDefaultLoggers >>>
          zio.logging.consoleLogger(
            ConsoleLoggerConfig.default.copy(
              format = LogFormat.colored,
              filter = LogFilter.logLevel(LogLevel.Info)
            )
          ) >>> zio.logging.slf4j.bridge.Slf4jBridge.initialize
      }
}

object AppConfig {

  val configProvider: ConfigProvider =
    zio.ConfigProvider.fromEnv(pathDelim = "_").snakeCase.upperCase orElse
      TypesafeConfigProvider
        .fromHoconFilePath(
          "conf/application.local.conf" /* development settings */
        )
        .kebabCase orElse
      TypesafeConfigProvider
        .fromHoconFilePath(
          "conf/application.conf" /* production */
        )
        .kebabCase orElse
      TypesafeConfigProvider.fromResourcePath().kebabCase
}
