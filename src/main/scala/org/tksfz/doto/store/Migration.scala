package org.tksfz.doto.store

import java.nio.file.Paths

import better.files.File
import io.circe.{Json, JsonObject}

import scala.collection.SortedMap

case class Migration(message: String, migrate: JsonObject => JsonObject)

/**
  * Enables stores that support migrations.
  */
trait Migratable extends Files with Yaml {

  /**
    * Note that newly created nodes get versionValue
    * while migrated nodes get max migration version
    */
  override protected def beforePut(json: Json): Json = {
    if (versionStore.option.isEmpty) {
      // Initialize the version store lazily. Alternatively we could do this on project init instead.
      versionStore.put(version)
    }
    // TODO: switch Coll to using circe's ObjectEncoder
    Json.fromJsonObject(
      super.beforePut(json).asObject.getOrElse(JsonObject.empty)
        .add(versionField, Json.fromInt(version))
    )
  }

  protected def versionFieldAndValue: (String, Int)

  private[this] def version = versionFieldAndValue._2

  /** Field in each document that stores the schema version for that document */
  private[this] def versionField: String = versionFieldAndValue._1

  /** File that stores the min schema version over all documents in this store.
    * Optimizes the check for out-of-date documents. */
  def versionFile = "version"

  /**
    * This is likely inadequate in the face of multi-user concurrency and will
    * need to be removed in the future in favor of using the index.
    */
  private[this] val versionStore = new SingletonsStore(root).singleton[Int](versionFile)

  def minDocVersion = versionStore.option.getOrElse(0)

  /**
    * @return relative paths of all (recursive) non-directory children
    */
  override protected def allDocPaths = {
    super.allDocPaths.filter(_ != Paths.get(versionFile))
  }

  /**
    * TODO: consider when minDocVersion > maxVersion. In that case we must prompt the user
    * to upgrade.
    */
  case class CheckableMigrations(migrations: SortedMap[Int, Migration]) {
    lazy val neededMigrations = migrations.dropWhile(_._1 <= minDocVersion)

    def run() = {
      val newMinDocVersion = (Migratable.this.allDocPaths.map { path =>
        val file = File(Migratable.this.root.resolve(path))
        val originalYamlStr = file.contentAsString
        fromYamlStr(originalYamlStr).flatMap(_.as[JsonObject]).map { originalJson =>
          val fromVersion = version(originalJson)
          val migrationsToRun = migrations.dropWhile(_._1 <= fromVersion)
          val newJson = migrationsToRun.foldLeft(originalJson) { case (fromJson, (version, migration)) =>
            migration.migrate(fromJson)
              .add(versionField, Json.fromInt(version))
          }
          val yamlStr = toYamlStr(Json.fromJsonObject(newJson))
          file.overwrite(yamlStr)
          version(newJson)
        }.getOrElse {
          // TODO: log
          new IllegalStateException(file + " " + originalYamlStr).printStackTrace()
          0
        }
      } :+ migrations.keys.max).min // empty Colls are given max migration version on upgrade
      // If we get to the end, then we must have maxVersion in all files
      assert(newMinDocVersion == migrations.keys.max)
      versionStore.put(newMinDocVersion)
    }
  }

  def checkMigrations(migrations: SortedMap[Int, Migration]) = CheckableMigrations(migrations)

  private[this] def version(obj: JsonObject) = {
    obj(versionField).flatMap(_.asNumber).flatMap(_.toInt).getOrElse(0)
  }

}

