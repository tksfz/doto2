package org.tksfz.doto.main

import java.time.{Duration, Instant}

import org.tksfz.doto.model.Status

object StatusCmdExec extends CmdExec[StatusCmd] {
  override def execute(c: Config, cmd: StatusCmd): Unit = WithActiveGitBackedProject { gbp =>
    val now = Instant.now()
    val email = gbp.git.getRepository.getConfig.getString("user", null, "email")
    cmd.activities foreach { case (id, msg) =>
      val expire = now.plus(Duration.ofHours(24))
      gbp.statuses.put(email + "/" + id.toString, Status(email, now, now, expire, msg))
    }
    gbp.commitAllIfNonEmpty(c.originalCommandLine)
  }
}
