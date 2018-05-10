package org.tksfz.doto.project

import java.io.File

import scala.sys.process._

/**
  * Wrapper around git cli invocations. Fallback for when jgit won't work under Graal AOT. In particular,
  * remote operations don't work because SubstrateVM won't support JCE until this issue is resolved:
  *
  * https://github.com/oracle/graal/issues/392
  */
class GitCli(cwd: File) {

  def exec(args: String*)= {
    val p = Process("git" +: args, cwd)
    p.run(true).exitValue()
  }

}
