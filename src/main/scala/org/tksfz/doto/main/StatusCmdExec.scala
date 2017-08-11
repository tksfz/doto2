package org.tksfz.doto.main

import java.time.{Duration, Instant}

import org.tksfz.doto.model.{Status, WhoWhat}

object StatusCmdExec extends CmdExec[StatusCmd] {
  override def execute(c: Config, cmd: StatusCmd): Unit = WithActiveGitBackedProject { gbp =>
    val now = Instant.now()
    val email = gbp.git.getRepository.getConfig.getString("user", null, "email")
    cmd.activities foreach { case (idStr, msg) =>
      gbp.findTaskOrEventByIdPrefix(idStr) map { work =>
        val expire = now.plus(Duration.ofHours(24))
        gbp.statuses.put(WhoWhat(email, work.id), Status(email, now, now, expire, msg))
      } getOrElse {
        Left("Couldn't find id")
      }
    }
    gbp.commitAllIfNonEmpty(c.originalCommandLine)
  }
}
