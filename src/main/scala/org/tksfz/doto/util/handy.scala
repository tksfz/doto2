package org.tksfz.doto.util

/**
  * Some handy utilities, with a functional bent
  *
  * Created by thom on 3/18/17.
  */
object handy {
  implicit class RichBoolean(val b: Boolean) extends AnyVal {
    final def thenSome[A](a: => A): Option[A] = if (b) Some(a) else None
  }
}
