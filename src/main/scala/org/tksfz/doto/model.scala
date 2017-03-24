package org.tksfz.doto

trait HasId { def id: Id }

sealed abstract class Ref[T <: HasId] {
  def id: Id

  def toIdRef: IdRef[T] = this match {
    case x@IdRef(_) => x
    case ValueRef(t) => IdRef(t.id)
  }
}
case class IdRef[T <: HasId](override val id: Id) extends Ref[T]
case class ValueRef[T <: HasId](t: T) extends Ref[T] {
  override def id = t.id
}

// TODO: consider pushing T down to a type member
trait HasChildren[T <: HasId] {
  type Self <: HasChildren[T] with Node[T] // dynamicPut(..) requires Node

  def children: List[Ref[T]]

  def withChildren(newChildren: List[Ref[T]]): Self

  def modifyChildren(f: List[Ref[T]] => List[Ref[T]]) = withChildren(f(children))
}

trait Completable { def completed: Boolean }

sealed abstract class Node[T <: HasId] extends HasId with Completable with HasChildren[T] {
  type Self <: Node[T]

  def subject: String

  def withCompleted(f: Boolean): Self
}

sealed abstract class Work extends DrawableNode[Task] {
  def target: Option[Ref[Event]]
}

trait DrawableNode[T <: HasId] extends Node[T] {
  def subjectColor: String
  def coloredSubject: String = if(Drawable.allowAnsi) subjectColor + subject + Console.RESET else subject
}

object Drawable {
  def allowAnsi: Boolean = System.console() != null
}

trait Drawable {
  def icon: String
  def iconColor: String
  def coloredIcon: String = if(Drawable.allowAnsi) iconColor + icon + Console.RESET else icon
}

case class Task(
  override val id: Id,
  override val subject: String,
  override val completed: Boolean = false,
  override val target: Option[Ref[Event]] = None,
  override val children: List[Ref[Task]] = Nil
) extends Work with Drawable {
  type Self = Work
  override def withCompleted(f: Boolean): Task = this.copy(completed = f)
  override def withChildren(newChildren: List[Ref[Task]]): Task = this.copy(children = newChildren)
  override def icon: String = if(completed) "[x]" else "[ ]"
  override def iconColor: String = if(completed) Console.GREEN else Console.RED + Console.BOLD
  override def subjectColor: String = if(completed) Console.GREEN else Console.RED + Console.BOLD
}

/**
  * Effectively, changes are buffered.
  */
case class Event(
  override val id: Id,
  //val when: Instant,
  override val subject: String,
  override val completed: Boolean = false,
  override val target: Option[Ref[Event]] = None,
  override val children: List[Ref[Task]] = Nil
) extends Work with Drawable {
  type Self = Event
  override def withCompleted(f: Boolean): Event = this.copy(completed = f)
  override def withChildren(newChildren: List[Ref[Task]]): Event = this.copy(children = newChildren)

  override def icon: String = if(completed) "![x]" else "![ ]"
  override def iconColor: String = if(completed) Console.YELLOW else Console.RED + Console.BOLD
  override def subjectColor: String = if(completed) Console.YELLOW else Console.RED + Console.BOLD
}

/** For encoding, we want a non-generic type */
sealed trait WorkType { def value: String; def threadIcon: String }
case object TaskWorkType extends WorkType { def value = "task"; def threadIcon = "~~" }
case object EventWorkType extends WorkType { def value = "event"; def threadIcon = "~~!" }

/** For constructor, we want a type class so that we don't need to pass in the WorkType explicitly */
trait WorkTypeClass[T] { def apply: WorkType }
object WorkTypeClass {
  implicit object TaskWorkTypeInstance extends WorkTypeClass[Task] { def apply = TaskWorkType }
  implicit object EventWorkTypeInstance extends WorkTypeClass[Event] { def apply = EventWorkType }
}

/**
  * Threads are a way of organizing Work
  *
  * Where do we put automatic completion conditions?
  */
// Thread +T
//case object RootThread extends Thread[Nothing] {

case class Thread[T <: Work](
  override val id: Id,
  /** Task/Event Threads can be parented to Threads of a different type */
  parent: Option[Ref[Thread[_]]],
  override val subject: String,
  override val completed: Boolean = false,
  override val children: List[Ref[T]] = Nil
)(implicit val `type`: WorkTypeClass[T]) extends DrawableNode[T] {
  type Self = Thread[T]

  def workType = this.`type`.apply

  def asTaskThread: Option[Thread[Task]] = TaskThread.unapply(this)

  def asEventThread: Option[Thread[Event]] = EventThread.unapply(this)

  override def withCompleted(f: Boolean): Thread[T] = this.copy(completed = f)
  override def withChildren(newChildren: List[Ref[T]]): Thread[T] = this.copy(children = newChildren)

  override def subjectColor: String = Console.BLUE
}

object EventThread {
  def unapply(t: Thread[_]): Option[Thread[Event]] = t.workType match {
    case TaskWorkType => None
    case EventWorkType => Some(t.asInstanceOf[Thread[Event]])
  }
}

object TaskThread {
  def unapply(t: Thread[_]): Option[Thread[Task]] = t.workType match {
    case TaskWorkType => Some(t.asInstanceOf[Thread[Task]])
    case EventWorkType => None
  }
}

import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import org.tksfz.doto.WorkTypeClass.{EventWorkTypeInstance, TaskWorkTypeInstance}

object Ref {
  implicit def idRefEncoder[T <: HasId]: Encoder[IdRef[T]] = deriveEncoder[IdRef[T]]
  implicit def idRefDecoder[T <: HasId]: Decoder[IdRef[T]] = deriveDecoder[IdRef[T]]

  implicit def refEncoder[T <: HasId]: Encoder[Ref[T]] = Encoder[IdRef[T]].contramap(_.toIdRef)

  // do we need this?
  implicit def refDecoder[T <: HasId]: Decoder[Ref[T]] = Decoder[IdRef[T]].map(identity)

  implicit class RefList[T <: HasId](val refs: List[Ref[T]]) extends AnyVal {
    def toIds: List[Id] = refs.map(_.id)
  }
}

object Task {
  implicit val taskEncoder = deriveEncoder[Task]
  implicit val taskDecoder = deriveDecoder[Task]
}

object Event {
  implicit val taskEncoder = deriveEncoder[Event]
  implicit val taskDecoder = deriveDecoder[Event]
}

object WorkType {
  def fromString(s: String) = s match {
    case "task" => TaskWorkType
    case "event" => EventWorkType
    case _ => throw new IllegalArgumentException("unrecognized work type '" + s + "'")
  }

  implicit val encoder: Encoder[WorkType] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[WorkType] = Decoder.decodeString.map(WorkType.fromString)
}

object Thread {
  /**
    * The `Nothing`s here prevent diverging implicit expansions
    */
  implicit def threadEncoder[T <: Work]: Encoder[Thread[T]] =
    Encoder.forProduct6("id", "parent", "subject", "completed", "children", "type")(u =>
      (u.id, u.parent.map(_.asInstanceOf[Ref[Nothing]]), u.subject, u.completed, u.children, u.`type`.apply)
    )

  // Using `b: Option[IdRef[Thread[T]]]` is not accurate but gets around implicit divergence
  implicit def threadDecoder[T <: Work : WorkTypeClass]: Decoder[Thread[T]] =
    Decoder.forProduct5("id", "parent", "subject", "completed", "children") {
      (a: Id, b: Option[IdRef[Thread[T]]], c: String, d: Boolean, e: List[Ref[T]]) => Thread.apply(a, b.map(_.asInstanceOf[IdRef[Thread[_]]]), c, d, e)
    }

  // existential
  implicit def threadEncoderExistential = new Encoder[Thread[_ <: Work]] {
    override def apply(a: Thread[_ <: Work]): Json = {
      a.`type`.apply match {
        case TaskWorkType => Encoder[Thread[Task]].apply(a.asInstanceOf[Thread[Task]])
        case EventWorkType => Encoder[Thread[Event]].apply(a.asInstanceOf[Thread[Event]])
      }
    }
  }

  implicit def threadDecoderExistential = new Decoder[Thread[_ <: Work]] {
    override def apply(c: HCursor): Result[Thread[_ <: Work]] = {
      c.downField("type").success.map { typ =>
        Decoder[WorkType].apply(typ).flatMap(_ match {
          case TaskWorkType => Decoder[Thread[Task]].apply(c)
          case EventWorkType => Decoder[Thread[Event]].apply(c)
        })
      } getOrElse {
        Left(DecodingFailure("Missing `type` on thread", c.history))
      }
    }
  }
}