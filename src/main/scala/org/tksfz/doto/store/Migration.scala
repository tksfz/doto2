package org.tksfz.doto.store

import better.files.File
import io.circe.{Json, JsonObject}

case class Migration(version: Int, migrate: JsonObject => JsonObject)

/**
  * Enables stores that support migrations.
  */
trait Migratable extends Files with Yaml {

  /** Field in each document that stores the schema version for that document */
  def versionField: String

  /** File that stores the min schema version over all documents in this store.
    * Optimizes the check for out-of-date documents. */
  def versionFile = "version"

  /**
    * This is likely inadequate in the face of multi-user concurrency and will
    * need to be removed in the future in favor of using the index.
    */
  private[this] val versionStore = new SingletonsStore(root).singleton[Int](versionFile)

  /**
    * @return relative paths of all (recursive) non-directory children
    *
    * TODO: rename to all doc children
    */
  override protected def allDocPaths = {
    super.allDocPaths.filter(_ != File(versionFile).path)
  }

  def checkAndRunMigrations(migrations: Seq[Migration]) = {
    val maxVersion = migrations.map(_.version).max

    val minDocVersion = versionStore.option.getOrElse(0)
    if (minDocVersion < maxVersion) {
      val newMinDocVersion = this.allDocPaths.map { path =>
        val file = File(this.root.resolve(path))
        val originalYamlStr = file.contentAsString
        fromYamlStr(originalYamlStr).flatMap(_.as[JsonObject]).map { originalJson =>
          val fromVersion = version(originalJson)
          val migrationsToRun = migrations.sortBy(_.version).dropWhile(_.version <= fromVersion)
          val newJson = migrationsToRun.foldLeft(originalJson) { (fromJson, migration) =>
            migration.migrate(fromJson)
              .add(versionField, Json.fromInt(migration.version))
          }
          val yamlStr = toYamlStr(Json.fromJsonObject(newJson))
          file.overwrite(yamlStr)
          version(newJson)
        }.getOrElse {
          // TODO: log
          0
        }
      }.min
      // TODO: consider when minDocVersion > maxVersion. In that case we must prompt the user
      // to upgrade.
      // If we get to the end, then we must have maxVersion in all files
      assert(newMinDocVersion == maxVersion)
      versionStore.put(newMinDocVersion)
    }
  }

  private[this] def version(obj: JsonObject) = {
    obj(versionField).flatMap(_.asNumber).flatMap(_.toInt).getOrElse(0)
  }

}

