package org.tksfz.doto.store

import better.files.File
import io.circe.{Json, JsonObject}

case class Migration(version: Int, migrate: JsonObject => JsonObject)

/**
  * Enables stores that support migrations.
  */
trait Migratable extends Files with Yaml {

  def versionField: String

  def versionFile = "version"

  private[this] val versionStore = new SingletonsStore(root).singleton[Int](versionFile)

  /**
    * @return relative paths of all (recursive) non-directory children
    *
    * TODO: rename to all doc children
    */
  override protected def allFileChildren = {
    super.allFileChildren.filter(_ != File(versionFile).path)
  }

  def checkAndRunMigrations(migrations: Seq[Migration]) = {
    val maxVersion = migrations.map(_.version).max

    val minDocVersion = versionStore.option.getOrElse(0)
    if (minDocVersion < maxVersion) {
      this.allFileChildren.map { path =>
        val file = this.root / path.toString
        val originalYamlStr = file.contentAsString
        val originalJson = fromYamlStr(originalYamlStr).right.get.asObject.get
        val fromVersion = originalJson(versionField).flatMap(_.asNumber).flatMap(_.toInt).getOrElse(0)
        val migrationsToRun = migrations.sortBy(_.version).dropWhile(_.version <= fromVersion)
        val toJson = migrationsToRun.foldLeft(originalJson) { (fromJson, migration) =>
          migration.migrate(fromJson)
            .add(versionField, Json.fromInt(migration.version))
        }
        val yamlStr = toYamlStr(Json.fromJsonObject(toJson))
        file.overwrite(yamlStr)
      }
    }
  }

}

