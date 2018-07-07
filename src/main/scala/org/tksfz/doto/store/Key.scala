package org.tksfz.doto.store

import java.nio.file.Path

abstract class HasKey[T, K](implicit val key: Key[K]) {
  def key(t: T): K
}

trait Key[K] {
  def toPath(k: K): Path
  def fromPath(p: Path): K
}
