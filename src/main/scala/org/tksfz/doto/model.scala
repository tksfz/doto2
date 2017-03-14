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

trait HasChildren[T <: HasId] { def children: List[Ref[T]] }

trait Completable { def completed: Boolean }

sealed abstract class Node[T <: HasId] extends HasId with Completable with HasChildren[T] {
  def subject: String
}

sealed abstract class Work extends Node[Task] {
  def target: Option[Ref[Event]]
}

case class Task(
  override val id: Id,
  override val completed: Boolean,
  override val target: Option[Ref[Event]],
  override val subject: String,
  override val children: List[Ref[Task]] = Nil
) extends Work

/**
  * Effectively, changes are buffered.
  */
case class Event(
  override val id: Id,
  override val completed: Boolean,
  override val target: Option[Ref[Event]],
  override val subject: String,
  override val children: List[Ref[Task]] = Nil
) extends Work

/**
  * Threads are a way of organizing Work
  *
  * Where do we put automatic completion conditions?
  */
// Thread +T
//case object RootThread extends Thread[Nothing] {

case class Thread[T <: Work](
  override val id: Id,
  override val completed: Boolean,
  parent: Option[Ref[Thread[T]]],
  override val subject: String,
  override val children: List[Ref[T]] = Nil
) extends Node[T]

import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._

object Ref {
  implicit def idRefEncoder[T <: HasId]: Encoder[IdRef[T]] = deriveEncoder[IdRef[T]]
  implicit def idRefDecoder[T <: HasId]: Decoder[IdRef[T]] = deriveDecoder[IdRef[T]]

  implicit def refEncoder[T <: HasId]: Encoder[Ref[T]] = new Encoder[Ref[T]] {
    override def apply(a: Ref[T]): Json = Encoder[IdRef[T]].apply(a.toIdRef)
  }

  // do we need this?
  implicit def refDecoder[T <: HasId]: Decoder[Ref[T]] = new Decoder[Ref[T]] {
    override def apply(c: HCursor): Result[Ref[T]] = Decoder[IdRef[T]].apply(c)
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

object Thread {
  implicit def threadEncoder[T <: Work : Encoder] = deriveEncoder[Thread[T]]
  implicit def threadDecoder[T <: Work : Encoder] = deriveDecoder[Thread[T]]
}