package org.tksfz.doto.project

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import org.eclipse.jgit.api._
import org.eclipse.jgit.api.errors.{EmtpyCommitException, TransportException}
import org.eclipse.jgit.transport.{PushResult, RemoteConfig, URIish}

import scala.util.Try

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

  def setRemote(url: URL) = {
    val cmd = git.remoteSetUrl()
    cmd.setName("origin")
    cmd.setUri(new URIish((url)))
    cmd.call()
  }

  def hasRemote = {
    !git.remoteList().call().isEmpty
  }

  import scala.collection.JavaConverters._

  def sync(noPull: Boolean) = {
    Try {
      val pullResult =
        if (!noPull) {
          Some(git.pull().setRebase(true).call())
        } else {
          None
        }
      // TODO: print out what was synced if possible
      val pushResult = git.push().call().asScala
      val remotes = git.remoteList().call().asScala
      val remote = pushResult.head.getURI.toASCIIString
      SyncResult(remote, pullResult, pushResult)
    }
  }
}

case class SyncResult(remote: String, pullResult: Option[PullResult], pushResult: Iterable[PushResult])

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