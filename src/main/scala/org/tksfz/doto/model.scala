package org.tksfz.doto

trait HasId { def id: Id }

sealed abstract class Ref[T <: HasId]
case class IdRef[T <: HasId](id: Id) extends Ref[T]
case class ValueRef[T <: HasId](t: T) extends Ref[T]

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
case class Thread[T <: Work](
  override val id: Id,
  override val completed: Boolean,
  parent: Option[Ref[Thread[T]]],
  override val subject: String,
  override val children: List[Ref[T]] = Nil
) extends Node[T]

