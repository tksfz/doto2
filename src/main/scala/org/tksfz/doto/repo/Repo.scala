package org.tksfz.doto.repo

import better.files.{File => ScalaFile, _}
import java.nio.file.Path
import java.util.UUID

import io.circe._
import io.circe.syntax._
import io.circe.yaml
import org.tksfz.doto.{Event, EventWorkType, HasId, Id, Ref, Task, Thread, Work}

object Repo {
  def init(rootPath: Path): Repo = {
    new Repo(rootPath)
  }
}

class Repo(rootPath: Path) {
  val root = ScalaFile(rootPath)

  private[this] val rootFile = root / "root"

  val threads = new ThreadColl(root / "threads")

  val tasks = new Coll[Task](root / "tasks")

  val events = new Coll[Event](root / "events")

  lazy val rootThread: Thread[Task] = {
    val id = UUID.fromString(rootFile.contentAsString)
    threads.get(id).toOption.get.asInstanceOf[Thread[Task]]
  }

  lazy val allThreads: Seq[Thread[_ <: Work]] = threads.findAll

  def findSubThreads(parentId: Id): Seq[Thread[_ <: Work]] = {
    allThreads filter { _.parent.map(_.id == parentId).getOrElse(false) }
  }

  /**
    * This is a critical function, and this implementation is temporary.
    *
    * Tasks are listed grouped by event. In the general case, tasks could be assigned to events from more than
    * one event thread. Within an event thread, there is a strict ordering (even without dates), but across event
    * threads can we define or find a strict ordering?
    */
  lazy val eventsOrdering: Ordering[Event] = {
    val allEvents = threads.findAllEventThreads.flatMap(t => events.findByRefs(t.children))
    orderingFromSeq(allEvents)
  }

  private[this] def orderingFromSeq[T](xs: Seq[T]): Ordering[T] = new Ordering[T] {
    // TODO: missing events?
    val order = xs.zipWithIndex.toMap
    override def compare(x: T, y: T): Int = order(x) - order(y)
  }
}

/**
  * A collection of objects, all of the same type
  */
class Coll[T : Encoder : Decoder](root: ScalaFile) {

  def findByIdPrefix(idPrefix: String): Option[T] = {
    findAllIds.find(_.toString.startsWith(idPrefix)).flatMap(get(_).toOption)
  }

  lazy val findAllIds: Seq[Id] = root.children.map(f => UUID.fromString(f.name)).toList

  def findAll: Seq[T] = findByIds(findAllIds)

  def findByIds(ids: Seq[Id]) = ids.map(get(_).toTry.get)

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

  // TODO: move Ref to here
  // assuming everything is IdRef here
  def findByRefs[A <: HasId](refs: Seq[Ref[A]])(implicit ev: A =:= T): Seq[T] = {
    findByIds(refs.map(_.id))
  }
}

class ThreadColl(root: ScalaFile) extends Coll[Thread[_ <: Work]](root) {
  def findAllEventThreads = {
    findAll.collect({ case eventThread: Thread[Event @unchecked] if eventThread.workType == EventWorkType => eventThread})
  }
}