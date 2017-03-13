package org.tksfz.doto.repo

import better.files.{File => ScalaFile, _}
import java.nio.file.Path

import io.circe.Encoder
import io.circe.syntax._
import io.circe.yaml._
import org.tksfz.doto.Id

class Repo(rootPath: Path) {
  val root = ScalaFile(rootPath)

  def put[T : Encoder](id: Id, doc: T) = {
    val json = doc.asJson
    val str = Printer().pretty(json)
    val file = root / id.toString
    file.overwrite(str)
  }
}
