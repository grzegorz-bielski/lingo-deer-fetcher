//> using scala "3.1.1"
//> using lib "com.softwaremill.sttp.client3::core:3.5.1"
//> using lib "com.softwaremill.sttp.client3::async-http-client-backend-fs2:3.5.1"

import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.*
import sttp.capabilities.fs2.Fs2Streams

import cats.syntax.all.given
import cats.{Traverse, Parallel}

import cats.effect.{IO, IOApp, Concurrent}
import cats.effect.std.{Console, Semaphore}

import fs2.{Stream, text}
import fs2.io.file.{Files, Path}

val jpLevelTwoTips =
  (1 to 62).map(n => Tip(level = 2, lesson = n, language = "jp")).toVector

object App extends IOApp.Simple:
  def run = TipsFetcher.run(jpLevelTwoTips).void

case class Tip(level: Int, lesson: Int, language: Tip.Language):
  def dirOutput = s"output/${language}/${level}"
  def fileOutput = s"${dirOutput}/${lesson}.html"
  def src =
    uri"https://webjson.lingodeer.com/dataSource/level_${level}/${language}/grammarTip/${lesson}/en.html"

object Tip:
  type Language = "jp"

object TipsFetcher:
  val parrarelism = 10

  def run(tips: Vector[Tip]) =
    AsyncHttpClientFs2Backend.resource[IO]().use { backend =>
      fetchTips(backend)(tips)
    }

  private type Backend[F[_]] = SttpBackend[F, Fs2Streams[F]]

  private def fetchTips[F[_]: Files: Concurrent: Parallel: Console](
      backend: Backend[F]
  )(tips: Vector[Tip]): F[Unit] =
    tips
      .parTraverseN(parrarelism)(fetchTip(backend))
      .void
      .handleErrorWith { e =>
        Console[F].println(s"errored: $e")
      }

  private def fetchTip[F[_]: Files: Concurrent: Console](
      backend: Backend[F]
  )(tip: Tip) =
    for
      _ <- Files[F].createDirectories(Path(tip.dirOutput))
      _ <- Console[F].println(s"started fetching ${tip}")
      _ <- basicRequest
        .get(tip.src)
        .response(asStreamAlways(Fs2Streams[F]) {
          _.chunks
            .through(text.utf8DecodeC)
            .through(text.utf8.encode)
            .through(Files[F].writeAll(Path(tip.fileOutput)))
            .compile
            .drain
        })
        .send(backend)
        .void
      _ <- Console[F].println(s"finished fetching ${tip}")
    yield ()

  extension [F[_]: Concurrent: Parallel, G[_]: Traverse, A](ga: G[A])
    def parTraverseN[B](n: Int)(f: A => F[B]) =
      Semaphore[F](n).flatMap { s =>
        ga.parTraverse { a =>
          for
            _ <- s.acquire
            b <- f(a)
            _ <- s.release
          yield b
        }
      }
