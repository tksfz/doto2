package org.tksfz.doto.main

import org.tksfz.doto.model.Id
import org.tksfz.doto.project.Project

trait ProjectExtensionsImplicits {

  implicit class ProjectExtensions(val project: Project) {
    val focus = project.unsynced.singleton[Id]("focus")
    val focusExcludes = project.unsynced.singleton[Seq[Id]]("focus.excludes")
  }
}
