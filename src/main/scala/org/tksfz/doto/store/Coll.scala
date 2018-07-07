package org.tksfz.doto.store

import java.nio.file.Path

import better.files.Dsl._
import better.files.File
import io.circe._
import io.circe.syntax._
import io.circe.yaml.Printer.StringStyle

class Coll[K, T : Encoder : Decoder](root: Path)(implicit hasKey: HasKey[T, K])
  extends { implicit val key = hasKey.key } with MapColl[K, T](root) {
  def put(doc: T): Unit = {
    put(hasKey.key(doc), doc)
  }
}

trait Files {
  def root: Path

  private[this] lazy val rootFile = File(root)

  /**
    * @return relative paths of all (recursive) non-directory children
    */
  protected def allDocPaths: Seq[Path] = {
    if (rootFile.exists) {
      rootFile.walk().filter(!_.isDirectory).filter(!_.isHidden).map(rootFile.relativize).toSeq
    } else {
      Nil
    }
  }
}

trait Yaml {
  protected final def fromYamlStr(yamlStr: String) = yaml.parser.parse(yamlStr)

  protected final def toYamlStr(json: Json) = yaml.Printer(stringStyle = StringStyle.Literal).pretty(json)
}

/**
  * A collection of objects, all of the same type
  */
class MapColl[K, T : Encoder : Decoder](val root: Path)(implicit key: Key[K])
  extends Files with Yaml {

  def findByIdPrefix(idPrefix: String): Option[T] = {
    findAllIds.find(_.toString.startsWith(idPrefix)).flatMap(get(_).toOption)
  }

  lazy val findAllIds: Seq[K] = {
    allDocPaths
      .map(f => key.fromPathString(f.toString))
      .toSeq
  }

  def count = findAllIds.size

  def findAll: Seq[T] = findByIds(findAllIds)

  // TODO: local indexing
  def findOne(f: T => Boolean): Option[T] = findAll.find(f)

  def findByIds(ids: Seq[K]) = ids.map(id => get(id).toTry.get)

  def get(id: K): Either[Error, T] = {
    val file = File(root.resolve(key.toPath(id)))
    val yamlStr = file.contentAsString
    val json = fromYamlStr(yamlStr)
    json.flatMap(_.as[T])
  }

  def put(id: K, doc: T): Unit = {
    val json = doc.asJson
    val yamlStr = toYamlStr(json)
    val file = File(root.resolve(key.toPath(id)))
    mkdirs(file.parent)
    file.overwrite(yamlStr)
  }

  def remove(id: K): Unit = {
    val file = File(root.resolve(key.toPath(id)))
    if (file.exists) {
      file.delete()
    }
  }

}
