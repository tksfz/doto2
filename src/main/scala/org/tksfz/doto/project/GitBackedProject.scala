package org.tksfz.doto.project

import java.io.File
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

class GitBackedProject(root: Path, val git: Git)
  extends Project(root) with Transactional with TransportHelpers {
  def this(root: Path) = this(root, Git.open(root.toFile))

  override def commitAllIfNonEmpty(msg: String) = {
    try {
      git.add().addFilepattern(".").call()
      val status = git.status().call()
      status.getMissing.forEach(x => git.rm().addFilepattern(x).call())
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
    GitBackedProject.setFetchRefSpec(git)
  }

  import scala.collection.JavaConverters._

  def hasRemote = remote.nonEmpty

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
          // TODO: we should be able to get rid of this in the future
          GitBackedProject.setFetchRefSpec(git)
          Some(git.pull().setupTransport().setRebase(true).call())
        } else {
          None
        }
      // TODO: print out what was synced if possible
      try {
        val pushResult = git.push().setupTransport().call().asScala
        SyncResult(pullResult, pushResult)
      } catch {
        case e: TransportException if e.getMessage contains "Nothing to push" =>
          SyncResult(pullResult, Nil)
      }
    }
  }

}


case class SyncResult(pullResult: Option[PullResult], pushResult: Iterable[PushResult])

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
    setFetchRefSpec(git)
    new GitBackedProject(location.toPath)
  }

  private[project] def setFetchRefSpec(git: Git) = {
    val config = git.getRepository.getConfig
    val remoteConfig = new RemoteConfig(config, "origin")
    remoteConfig.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/origin/*"))
    remoteConfig.update(config)
    config.save()
  }

  def open(location: Path): GitBackedProject = {
    new GitBackedProject(location)
  }

}