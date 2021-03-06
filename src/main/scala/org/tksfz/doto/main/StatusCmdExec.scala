package org.tksfz.doto.main

import java.time.{Duration, Instant}

import org.tksfz.doto.model.{Status, User, WhoWhat}

object StatusCmdExec extends CmdExec[StatusCmd] {
  override def execute(c: Config, cmd: StatusCmd): Unit = WithActiveGitBackedProject { gbp =>
    val now = Instant.now()
    // TODO: throw error message if email or name can't be found
    // https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/ReadUserConfig.java
    val email = Option(gbp.git.getRepository.getConfig.getString("user", null, "email")).get
    val name = Option(gbp.git.getRepository.getConfig.getString("user", null, "name")).get
    gbp.findTaskOrEventByIdPrefix(cmd.id) map { work =>
      val expire = now.plus(Duration.ofHours(24))
      gbp.statuses.put(Status(email, now, now, expire, User(name, email), work.id, cmd.status))
    } getOrElse {
      Left("Couldn't find id")
    }
    cmd.remove.foreach { idStr =>
      gbp.findTaskOrEventByIdPrefix(idStr) map { work =>
        val key = WhoWhat(email, work.id)
        gbp.statuses.remove(key)
      }
    }
    gbp.commitAllIfNonEmpty(c.originalCommandLine)
  }
}
