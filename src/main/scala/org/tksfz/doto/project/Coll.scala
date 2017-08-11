package org.tksfz.doto.project

import java.util.UUID

import better.files.{File => ScalaFile, _}
import better.files.Cmds
import io.circe._
import io.circe.syntax._
import org.tksfz.doto.model.{HasId, Id, Ref}

import scala.util.Try

class Coll[T : Encoder : Decoder](root: ScalaFile) extends MapColl[Id, T](root) {
  def put[A <: HasId](doc: A)(implicit ev: A =:= T): Unit = {
    put(doc.id, doc)
  }

  // TODO: move Ref to here
  // assuming everything is IdRef here
  def findByRefs[A <: HasId](refs: Seq[Ref[A]])(implicit ev: A =:= T): Seq[T] = {
    findByIds(refs.map(_.id))
  }
}

/**
  * A collection of objects, all of the same type
  */
class MapColl[K, T : Encoder : Decoder](root: ScalaFile)(implicit keyable: Keyable[K]) {

  def findByIdPrefix(idPrefix: String): Option[T] = {
    findAllIds.find(_.toString.startsWith(idPrefix)).flatMap(get(_).toOption)
  }

  lazy val findAllIds: Seq[K] = {
    allFileChildren
      .map(f => implicitly[Keyable[K]].fromString(f.toString))
      .toSeq
  }

  /**
    * @return relative paths of all non-directory children
    */
  private[this] def allFileChildren = {
    if (root.exists) {
      root.walk().filter(!_.isDirectory).filter(!_.isHidden).map(root.relativize)
    } else {
      Nil
    }
  }

  def count = findAllIds.size

  def findAllWithIds: Seq[(K, T)] = findWithIds(findAllIds)

  def findAll: Seq[T] = findByIds(findAllIds)

  // TODO: local indexing
  def findOne(f: T => Boolean): Option[T] = findAll.find(f)

  def findByIds(ids: Seq[K]) = findWithIds(ids).map(_._2)

  def findWithIds(ids: Seq[K]) = ids.map(id => id -> get(id).toTry.get)

  def get(id: K): Either[Error, T] = {
    val file = root / keyable.toString(id)
    val yamlStr = file.contentAsString
    val json = yaml.parser.parse(yamlStr)
    json.flatMap(_.as[T])
  }

  def put(id: K, doc: T): Unit = {
    val json = doc.asJson
    val yamlStr = yaml.Printer().pretty(json)
    val file = root / keyable.toString(id)
    Cmds.mkdirs(file.parent)
    file.overwrite(yamlStr)
  }

  def remove(id: K): Unit = {
    val file = root / keyable.toString(id)
    file.delete()
  }

}
