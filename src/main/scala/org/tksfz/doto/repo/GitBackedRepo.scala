package org.tksfz.doto.repo

import java.io.File
import java.net.{URI, URL}
import java.nio.file.Path

import org.eclipse.jgit.api._

class GitBackedRepo(val repo: Repo) {

}

object GitBackedRepo {
  def init(location: Path): GitBackedRepo = {
    ???
  }

  def clone(url: URI, location: File): GitBackedRepo = {
    val clone = new CloneCommand().setURI(url.toString).setDirectory(location)
    val git = clone.call()
    null
  }

  def find(): GitBackedRepo = {
    ???
  }
}