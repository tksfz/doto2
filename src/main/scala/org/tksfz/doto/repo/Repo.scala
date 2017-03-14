package org.tksfz.doto.repo

import better.files.{File => ScalaFile, _}
import java.nio.file.Path
import java.util.UUID

import io.circe._
import io.circe.syntax._
import io.circe.yaml
import org.tksfz.doto.{Id, Task, Thread, Work}

object Repo {
  def init(rootPath: Path): Repo = {
    new Repo(rootPath)
  }
}

class Repo(rootPath: Path) {
  val root = ScalaFile(rootPath)

  private[this] val rootFile = root / "root"

  val threads = new Coll[Thread[Task]](root / "threads")

  val tasks = new Coll[Task](root / "tasks")

  lazy val rootThread: Thread[Task] = {
    val id = UUID.fromString(rootFile.contentAsString)
    threads.get(id).toOption.get
  }

  lazy val allThreads: Seq[Thread[_ <: Work]] = threads.findAll

  def findSubThreads(parentId: Id): Seq[Thread[_ <: Work]] = {
    allThreads filter { _.parent.map(_.id == parentId).getOrElse(false) }
  }

}

/**
  * A collection of objects, all of the same type
  */
class Coll[T : Encoder : Decoder](root: ScalaFile) {

  def findByIdPrefix(idPrefix: String): Option[T] = {
    findAllIds.find(_.toString.startsWith(idPrefix)).flatMap(get(_).toOption)
  }

  lazy val findAllIds: Seq[Id] = root.children.map(f => UUID.fromString(f.name)).toSeq

  def findAll: Seq[T] = findByIds(findAllIds)

  def findByIds(ids: Seq[Id]) = ids.map(get(_).toOption).flatten

  def get(id: Id): Either[Error, T] = {
    val file = root / id.toString
    val yamlStr = file.contentAsString
    val json = yaml.parser.parse(yamlStr)
    json.flatMap(_.as[T])
  }

  def put(id: Id, doc: T) = {
    val json = doc.asJson
    val yamlStr = yaml.Printer().pretty(json)
    val file = root / id.toString
    file.overwrite(yamlStr)
  }
}