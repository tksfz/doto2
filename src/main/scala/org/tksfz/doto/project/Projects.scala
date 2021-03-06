package org.tksfz.doto.project

import better.files.{File => ScalaFile, _}
import org.tksfz.doto.store.SingletonsStore

/**
  * Created by thom on 3/23/17.
  */
object Projects {

  /** GraalVM AOT initializes this at build-time!
    * So we make it a lazy val here
    */
  lazy val DEFAULT_DOTO_HOME = ScalaFile.home / ".doto"

  def defaultProjectRoot(projectName: String) = DEFAULT_DOTO_HOME / projectName

  def listProjects: Seq[String] = {
    if (DEFAULT_DOTO_HOME.isDirectory) {
      DEFAULT_DOTO_HOME.children.filter(_.isDirectory).map(_.name).toSeq
    } else {
      Nil
    }
  }

  /** Again lazy to support GraalVM AOT */
  private[this] lazy val global = new SingletonsStore(DEFAULT_DOTO_HOME)

  lazy val activeProjectName = global.getSingleton[String]("active")

  def activeProject = activeProjectName map { name =>
    GitBackedProject.open(defaultProjectRoot(name).toJava.toPath)
  }

  def setActiveProject(n: String): Boolean = {
    if (listProjects.contains(n)) {
      global.putSingleton("active", n)
      true
    } else {
      false
    }
  }

  // TODO
  private[this] def isValidProjectRoot(location: ScalaFile) = {
    (location / "synced").exists
  }
}
