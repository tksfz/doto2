package org.tksfz.doto.project

import better.files.{File => ScalaFile, _}
import better.files.Cmds
import io.circe._
import io.circe.syntax._
import org.tksfz.doto.model.{HasId, Id, Ref}

class Coll[K, T : Encoder : Decoder](root: ScalaFile)(implicit hasKey: HasKey[T, K])
  extends { implicit val key = hasKey.key } with MapColl[K, T](root) {
  def put(doc: T): Unit = {
    put(hasKey.key(doc), doc)
  }
}

/**
  * A collection of objects, all of the same type
  */
class MapColl[K, T : Encoder : Decoder](root: ScalaFile)(implicit key: Key[K]) {

  def findByIdPrefix(idPrefix: String): Option[T] = {
    findAllIds.find(_.toString.startsWith(idPrefix)).flatMap(get(_).toOption)
  }

  lazy val findAllIds: Seq[K] = {
    allFileChildren
      .map(f => key.fromPathString(f.toString))
      .toSeq
  }

  /**
    * @return relative paths of all (recursive) non-directory children
    */
  private[this] def allFileChildren = {
    if (root.exists) {
      root.walk().filter(!_.isDirectory).filter(!_.isHidden).map(root.relativize)
    } else {
      Nil
    }
  }

  def count = findAllIds.size

  def findAll: Seq[T] = findByIds(findAllIds)

  // TODO: local indexing
  def findOne(f: T => Boolean): Option[T] = findAll.find(f)

  def findByIds(ids: Seq[K]) = ids.map(id => get(id).toTry.get)

  def get(id: K): Either[Error, T] = {
    val file = root / key.toPathString(id)
    val yamlStr = file.contentAsString
    val json = yaml.parser.parse(yamlStr)
    json.flatMap(_.as[T])
  }

  def put(id: K, doc: T): Unit = {
    val json = doc.asJson
    val yamlStr = yaml.Printer().pretty(json)
    val file = root / key.toPathString(id)
    Cmds.mkdirs(file.parent)
    file.overwrite(yamlStr)
  }

  def remove(id: K): Unit = {
    val file = root / key.toPathString(id)
    file.delete(true)
  }

}