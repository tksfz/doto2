package org.tksfz.doto.project

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import com.jcraft.jsch.Session
import org.eclipse.jgit.api._
import org.eclipse.jgit.api.errors.{EmtpyCommitException, TransportException}
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.transport._

import scala.util.Try

trait TransportHelpers {
  private[this] val sshSessionFactory = new JschConfigSessionFactory {
    override def configure(hc: OpenSshConfig.Host, session: Session): Unit = {}
  }

  // For now only support ssh url's
  implicit class TransportCommandExtensions[T <: TransportCommand[_, _]](val cmd: T) {
    def setupTransport() = {
      cmd.setTransportConfigCallback { _ match {
        case sshTransport: SshTransport => sshTransport.setSshSessionFactory(sshSessionFactory)
        case _ =>
      } }
      cmd
    }
  }
}

class GitBackedProject(root: Path, git: Git)
  extends Project(root) with Transactional with TransportHelpers {
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

  def setRemote(url: String) = {
    val cmd = git.remoteSetUrl()
    cmd.setName("origin")
    cmd.setUri(new URIish(url))
    cmd.call()
  }

  import scala.collection.JavaConverters._

  def hasRemote = {
    remote.nonEmpty
  }

  def remote = {
    val repo = git.getRepository
    val remote = new BranchConfig(repo.getConfig, repo.getBranch).getRemote
    git.remoteList.call().asScala.find(_.getName == remote) flatMap {
      _.getURIs.asScala.headOption
    }
  }

  def sync(noPull: Boolean) = {
    Try {
      val pullResult =
        if (!noPull) {
          Some(git.pull().setupTransport().setRebase(true).call())
        } else {
          None
        }
      // TODO: print out what was synced if possible
      val pushResult = git.push().setupTransport().call().asScala
      val remote = pushResult.head.getURI.toASCIIString
      SyncResult(remote, pullResult, pushResult)
    }
  }

}


case class SyncResult(remote: String, pullResult: Option[PullResult], pushResult: Iterable[PushResult])

object GitBackedProject extends TransportHelpers {
  def init(location: Path): GitBackedProject = {
    val init = new InitCommand().setDirectory(location.toFile)
    val git = init.call()
    new GitBackedProject(location, git)
  }

  def clone(url: String, location: File): GitBackedProject = {
    val clone =
      new CloneCommand()
        .setURI(url)
        .setupTransport()
        .setDirectory(location)
    val git = clone.call()
    new GitBackedProject(location.toPath)
  }

  def open(location: Path): GitBackedProject = {
    new GitBackedProject(location)
  }

}