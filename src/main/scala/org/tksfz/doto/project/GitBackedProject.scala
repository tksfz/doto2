package org.tksfz.doto.project

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import org.eclipse.jgit.api._
import org.eclipse.jgit.api.errors.EmtpyCommitException

class GitBackedProject(root: Path, git: Git) extends Project(root) with Transactional {
  def this(root: Path) = this(root, Git.open(root.toFile))

  override def commitAllIfNonEmpty(msg: String) = {
    try {
      val index = git.add().addFilepattern(".").call()
      // TODO: check if index empty
      git.commit()
        .setAllowEmpty(false) // causes EmptyCommitException to be thrown if there are no changes
        .setMessage(msg)
        .call()
      true
    } catch {
      case e: EmtpyCommitException => false
    }
  }

  def sync() = {
    // TODO: handle repos with no remote, and remote management
    if (git.remoteList().call().isEmpty()) {
      false
    } else {
      git.pull().setRebase(true).call()
      // TODO: print out what was synced if possible
      git.push().call()
      true
    }
  }
}

object GitBackedProject {
  def init(location: Path): GitBackedProject = {
    val init = new InitCommand().setDirectory(location.toFile)
    val git = init.call()
    new GitBackedProject(location, git)
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