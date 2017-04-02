package org.tksfz.doto.repo

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import org.eclipse.jgit.api._

class GitBackedProject(root: Path, git: Git) extends Repo(root) {
  def this(root: Path) = this(root, Git.open(root.toFile))
}

object GitBackedProject {
  def init(location: Path): GitBackedProject = {
    val init = new InitCommand().setDirectory(location.toFile)
    val git = init.call()
    null
  }

  def clone(url: URI, location: File): GitBackedProject = {
    val clone = new CloneCommand().setURI(url.toString).setDirectory(location)
    val git = clone.call()
    new GitBackedProject(location.toPath)
  }

  def open(location: Path): GitBackedProject = {
    new GitBackedProject(location)
  }
}