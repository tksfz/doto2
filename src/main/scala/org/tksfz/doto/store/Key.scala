package org.tksfz.doto.store

import java.nio.file.{Path, Paths}
import java.util.UUID

import org.tksfz.doto.model.Id

abstract class HasKey[T, K](implicit val key: Key[K]) {
  def key(t: T): K
}

trait Key[K] {
  def toPath(k: K): Path
  def fromPathString(s: String): K
}

object Key {
  implicit val idKey = new Key[Id] {
    override def toPath(k: Id): Path = Paths.get(k.toString)
    override def fromPathString(s: String): Id = UUID.fromString(s)
  }
}

