package org.tksfz.doto.store

import better.files.{File => ScalaFile}
import io.circe.{Decoder, Encoder, yaml}
import io.circe.syntax._

class SingletonsStore(root: ScalaFile) {
  class ForKey[T : Encoder : Decoder](key: String) {
    def option: Option[T] = getSingleton[T](key)

    def put(value: T) = putSingleton[T](key, value)

    def remove() = SingletonsStore.this.remove(key)
  }

  def singleton[T : Encoder : Decoder](key: String) = new ForKey[T](key)

  def getSingleton[T : Decoder](key: String): Option[T] = {
    val file = root / key
    if (file.exists) {
      Some(yaml.parser.parse((root / key).contentAsString).flatMap(_.as[T]).toTry.get)
    } else {
      None
    }
  }

  def putSingleton[T : Encoder](key: String, doc: T) = {
    val json = doc.asJson
    val yamlStr = yaml.Printer().pretty(json)
    val file = root / key
    file.overwrite(yamlStr)
  }

  def remove(key: String): Unit = {
    val file = root / key
    if (file.exists) {
      file.delete()
    }
  }
}
