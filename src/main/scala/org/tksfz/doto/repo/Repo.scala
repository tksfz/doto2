package org.tksfz.doto.repo

import better.files.{File => ScalaFile, _}
import java.nio.file.Path
import java.util.UUID

import io.circe._
import io.circe.syntax._
import io.circe.yaml
import org.tksfz.doto.{Id, Task, Thread}

object Repo {
  def init(rootPath: Path): Repo = {
    new Repo(rootPath)
  }
}

class Repo(rootPath: Path) {
  val root = ScalaFile(rootPath)

  private[this] val taskRootFile = root / "taskRoot"

  val taskThreads = new Coll[Thread[Task]](root / "taskThreads")

  lazy val rootTaskThread: Thread[Task] = {
    val id = UUID.fromString(taskRootFile.contentAsString)
    taskThreads.get(id).toOption.get
  }

  lazy val allTaskThreads: Seq[Thread[Task]] = taskThreads.findAll

  def findSubThreads(parentId: Id): Seq[Thread[Task]] = {
    allTaskThreads filter { _.parent.map(_.id == parentId).getOrElse(false) }
  }

}

/**
  * A collection of objects, all of the same type
  */
class Coll[T : Encoder : Decoder](root: ScalaFile) {

  def findAllIds: Seq[Id] = root.children.map(f => UUID.fromString(f.name)).toSeq

  def findAll: Seq[T] = findAllIds.map(get(_).toOption).flatten

  def get(id: Id): Either[Error, T] = {
    val file = root / id.toString
    val yaml = file.contentAsString
    val json = parser.parse(yaml)
    json.flatMap(_.as[T])
  }

  def put(id: Id, doc: T) = {
    val json = doc.asJson
    val yamlStr = yaml.Printer().pretty(json)
    val file = root / id.toString
    file.overwrite(yamlStr)
  }
}