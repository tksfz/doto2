package org.tksfz.doto.project

import java.util.UUID

import org.tksfz.doto.model.Id

trait Keyable[K] {
  def toString(k: K): String
  def fromString(s: String): K
}

object Keyable {
  implicit val stringKeyable = new Keyable[String] {
    override def toString(k: String): String = k
    override def fromString(s: String): String = s
  }

  implicit val idKeyable = new Keyable[Id] {
    override def toString(k: Id): String = k.toString
    override def fromString(s: String): Id = UUID.fromString(s)
  }
}

