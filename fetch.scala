// #!/usr/bin/env -S scala-cli shebang

//> using scala "3.1.1"
//> using lib "com.softwaremill.sttp.client3::core:3.5.1"
//> using lib "com.softwaremill.sttp.client3::async-http-client-backend-fs2:3.5.1"

import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend

import sttp.client3._
import sttp.capabilities.fs2.Fs2Streams
import cats.effect.{IO, IOApp}
import fs2.{Stream, text}
import fs2.io.file.{Files, Path}

def fetchTip(level: Int, lesson: Int)(backend: SttpBackend[IO, Fs2Streams[IO]]): IO[Unit] =
  basicRequest
      .get(
        uri"https://webjson.lingodeer.com/dataSource/level_${level}/jp/grammarTip/${lesson}/en.html"
      )
      .response(asStreamAlways(Fs2Streams[IO]) {
        _.chunks
          .through(text.utf8DecodeC)
          .through(text.utf8.encode)
          .through(Files[IO].writeAll(Path("celsius.txt")))
          .compile
          .drain
      })
      .send(backend)
      .void

val program = AsyncHttpClientFs2Backend.resource[IO]().use { backend => 
  
  fetchTip(level = 2, lesson = 4)(backend)
}

object App extends IOApp.Simple:
  def run = program.void
