package org.tksfz.doto

import java.nio.file.Paths
import java.util.UUID

import org.tksfz.doto.repo.Repo

object AddCommandExec extends CommandExec[Add] {
  override def execute(c: Config, t: Add): Unit = {
    val repo = new Repo(Paths.get(""))
    val parentUuid = UUID.randomUUID() // UUID.fromString(t.parentId)
    val uuid = UUID.randomUUID()
    val doc = Thread[Task](uuid, false, Some(IdRef(parentUuid)), t.subject)
    repo.taskThreads.put(uuid, doc)
  }
}
