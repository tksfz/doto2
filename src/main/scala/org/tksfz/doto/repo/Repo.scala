package org.tksfz.doto.repo

import better.files.{File => ScalaFile, _}
import java.nio.file.Path
import java.util.UUID

import io.circe._
import io.circe.syntax._
import io.circe.yaml
import org.tksfz.doto._

object Repo {
  def init(rootPath: Path): Repo = {
    new Repo(rootPath)
  }
}

class Repo(rootPath: Path) {
  val root = ScalaFile(rootPath)

  /**
    * Projects have two kinds of state: "synced" and "local". The intent is that synced state (tree of work, etc.)
    * is stored in git and local state (focus, etc.) is stored only locally.
    */
  private[this] val syncedRoot = root / "synced"

  private[this] val unsyncedRoot = root / "local"

  private[this] val rootFile = syncedRoot / "root"

  val threads = new ThreadColl(syncedRoot / "threads")

  val tasks = new Coll[Task](syncedRoot / "tasks")

  val events = new Coll[Event](syncedRoot / "events")

  lazy val rootThread: Thread[Task] = {
    val id = UUID.fromString(rootFile.contentAsString)
    threads.get(id).toTry.get.asInstanceOf[Thread[Task]]
  }

  lazy val allThreads: Seq[Thread[_ <: Work]] = threads.findAll

  def findTaskOrEventByIdPrefix(idPrefix: String): Option[_ <: Work] = {
    this.tasks.findByIdPrefix(idPrefix) orElse this.events.findByIdPrefix(idPrefix)
  }

  /**
    * These are things that can contain tasks
    */
  def findTaskOrTaskThreadByIdPrefix(idPrefix: String): Option[_ <: Node[Task]] = {
    // TODO: filter to task threads or only
    this.threads.findByIdPrefix(idPrefix).flatMap(_.asTaskThread) orElse this.tasks.findByIdPrefix(idPrefix)
  }

  def findOneTaskOrTaskThread(f: HasChildren[Task] => Boolean) = {
    this.threads.findAllTaskThreads.find(f) orElse this.tasks.findOne(f)
  }

  def findTaskParent(taskId: Id) = findOneTaskOrTaskThread(_.children.exists(_.id == taskId))

  trait CollByType[T] {
    def coll: Coll[T]
  }

  // Do these need to be inside object CollByType?
  object CollByType {
    implicit object threadsColl extends CollByType[Thread[_ <: Work]] { def coll = threads }
    implicit object eventsColl extends CollByType[Event] { def coll = events }
    implicit object tasksColl extends CollByType[Task] { def coll = tasks }
  }

  def put[T <: Node[_] : CollByType](t: T) = implicitly[CollByType[T]].coll.put(t)

  /**
    * Check the type of the value at runtime, then put it into the appropriate Coll.
    */
  def dynamicPut(t: Node[_]) = {
    t match {
      case thread: Thread[_] => put[Thread[_ <: Work]](thread)
      case task: Task => put(task)
      case event: Event => put(event)
    }
  }

  def findSubThreads(parentId: Id): Seq[Thread[_ <: Work]] = {
    allThreads filter { _.parent.map(_.id == parentId).getOrElse(false) }
  }

  val synced = new SingletonStore(syncedRoot)

  val unsynced = new SingletonStore(unsyncedRoot)

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

class SingletonStore(root: ScalaFile) {
  def getSingleton[T : Decoder](key: String): Option[T] = {
    val file = root / key
    if (file.exists) {
      Some(yaml.parser.parse((root / key).contentAsString).flatMap(_.as[T]).toTry.get)
    } else {
      None
    }
  }

  def putSingleton[T : Encoder](key: String, doc: T) = {
    val json = doc.asJson
    val yamlStr = yaml.Printer().pretty(json)
    val file = root / key
    file.overwrite(yamlStr)
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

  // TODO: local indexing
  def findOne(f: T => Boolean): Option[T] = findAll.find(f)

  def findByIds(ids: Seq[Id]) = ids.map(get(_).toTry.get)

  def get(id: Id): Either[Error, T] = {
    val file = root / id.toString
    val yamlStr = file.contentAsString
    val json = yaml.parser.parse(yamlStr)
    json.flatMap(_.as[T])
  }

  def put(id: Id, doc: T): Unit = {
    val json = doc.asJson
    val yamlStr = yaml.Printer().pretty(json)
    val file = root / id.toString
    file.overwrite(yamlStr)
  }

  def put[A <: HasId](doc: A)(implicit ev: A =:= T): Unit = {
    put(doc.id, doc)
  }

  // TODO: move Ref to here
  // assuming everything is IdRef here
  def findByRefs[A <: HasId](refs: Seq[Ref[A]])(implicit ev: A =:= T): Seq[T] = {
    findByIds(refs.map(_.id))
  }
}

class ThreadColl(root: ScalaFile) extends Coll[Thread[_ <: Work]](root) {
  def findAllEventThreads = findAll.collect { case EventThread(et) => et }

  def findAllTaskThreads = findAll.collect { case TaskThread(tt) => tt }
}