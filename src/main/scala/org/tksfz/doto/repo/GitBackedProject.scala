package org.tksfz.doto.repo

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import org.eclipse.jgit.api._

class GitBackedProject(val project: Repo, git: Git) {

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
    new GitBackedProject(new Repo(location.toPath), git)
  }

  def open(repo: Repo): GitBackedProject = {
    val git = Git.open(repo.syncedRoot.toJava)
    new GitBackedProject(repo, git)
  }
}