package org.tksfz.doto.project

import java.io.File

import better.files.{File => ScalaFile, _}
import java.nio.file.Path
import java.util.UUID

import io.circe._
import io.circe.syntax._
import io.circe.yaml
import org.tksfz.doto.model._
import org.tksfz.doto.store.{Coll, SingletonsStore}

object Project {
  def init(rootPath: Path): Project = {
    new Project(rootPath)
  }
}

class Project(rootPath: Path) {
  val root = ScalaFile(rootPath)

  def name = root.toJava.getName

  /**
    * Projects have two kinds of state: "synced" and "local". The intent is that synced state (tree of work, etc.)
    * is stored in git and local state (focus, etc.) is stored only locally.
    */
  val syncedRoot = root

  private[this] val unsyncedRoot = root / "local"

  private[this] val rootFile = syncedRoot / "root"

  val threads = new ThreadColl(syncedRoot / "threads")

  val tasks = new NodeColl[Task](syncedRoot / "tasks")

  val events = new NodeColl[Event](syncedRoot / "events")

  val statuses = new Coll[WhoWhat, Status]((syncedRoot / "statuses").path)

  lazy val rootThread: Thread[Task] = {
    val id = UUID.fromString(rootFile.contentAsString)
    threads.get(id).toTry.get.asInstanceOf[Thread[Task]]
  }

  lazy val allThreads: Seq[Thread[_ <: Work]] = threads.findAll

  def findNodeByIdPrefix(idPrefix: String): Option[Node[_ <: HasId]] = {
    findTaskOrEventByIdPrefix(idPrefix) orElse this.threads.findByIdPrefix(idPrefix)
  }

  def findTaskOrEventByIdPrefix(idPrefix: String): Option[Work] = {
    this.tasks.findByIdPrefix(idPrefix) orElse this.events.findByIdPrefix(idPrefix)
  }

  def find(f: HasChildren[_ <: HasId] => Boolean) = {
    val thread: Option[Node[_ <: HasId]] = this.allThreads.find(f)
    thread orElse this.tasks.findOne(f) orElse this.events.findOne(f)
  }

  def findParent(id: Id) = find(_.children.exists(_.id == id))

  trait CollByType[T <: Node[_]] {
    def coll: NodeColl[T]
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

  val synced = new SingletonsStore(syncedRoot)

  val unsynced = new SingletonsStore(unsyncedRoot)

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

class NodeColl[T <: Node[_] : Encoder : Decoder](root: ScalaFile)
  extends Coll[Id, T](root.path) {
  // assuming everything is IdRef here
  def findByRefs(refs: Seq[Ref[T]]): Seq[T] = {
    findByIds(refs.map(_.id))
  }
}

class ThreadColl(root: ScalaFile) extends NodeColl[Thread[_ <: Work]](root) {
  def findAllEventThreads = findAll.collect { case EventThread(et) => et }

  def findAllTaskThreads = findAll.collect { case TaskThread(tt) => tt }
}