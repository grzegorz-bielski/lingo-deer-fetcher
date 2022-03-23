#!/usr/bin/env -S scala-cli shebang

using scala "3.1.1"
using lib "com.softwaremill.sttp.client3::core:3.5.1"

import sttp.client3.quick._

def fetchTip(level: Int, lesson: Int) =
  quickRequest
    .get(
      uri"https://webjson.lingodeer.com/dataSource/level_${level}/jp/grammarTip/${lesson}/en.html"
    )
    .send(backend)
    .body

println(
  fetchTip(
    level = 2,
    lesson = 2
  )
)
