package som.storyforge.example

import zio._

import java.time.LocalDate

trait TimeRepository {
  def record(localDate: LocalDate): Task[Unit]
}

case class FakeTimeRepository() extends TimeRepository {
  override def record(localDate: LocalDate): Task[Unit] =
    zio.Console.printLine("fake record").unit
}
object FakeTimeRepository {
  val layer: ULayer[FakeTimeRepository] = ZLayer.succeed(FakeTimeRepository())
}
