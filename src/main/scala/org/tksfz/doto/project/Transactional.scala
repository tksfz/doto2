package org.tksfz.doto.project

/**
  * Created by thom on 4/1/17.
  */
trait Transactional { self: Project =>
  def commitAllIfNonEmpty(msg: String): Boolean
}
