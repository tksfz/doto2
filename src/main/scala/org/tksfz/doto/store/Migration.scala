package org.tksfz.doto.store

import io.circe.{Json, JsonObject, yaml}
import io.circe.yaml.Printer.StringStyle

case class Migration(version: Int, migrate: JsonObject => JsonObject)

/**
  * Note specifically we don't care about the MapColl types K and T.
  * Migrations operate at the JsonObject level and are oblivious to the user type T.
  */
trait MigrationSupport extends MapColl[_, _] {

  def versionField: String

  def checkAndRunMigrations(migrations: Seq[Migration]) = {
    val maxVersion = migrations.map(_.version).max

    val minDocVersion = 0
    if (minDocVersion < maxVersion) {
      this.allFileChildren.map { path =>
        val file = this.root / path.toString
        val yamlStr = file.contentAsString
        val originalJson = yaml.parser.parse(yamlStr).right.get.asObject.get
        val fromVersion = originalJson(versionField).flatMap(_.asNumber).flatMap(_.toInt).getOrElse(0)
        val migrationsToRun = migrations.sortBy(_.version).dropWhile(_.version <= fromVersion)
        val toJson = migrationsToRun.foldLeft(originalJson) { (fromJson, migration) =>
          migration.migrate(fromJson)
            .add(versionField, Json.fromInt(migration.version))
        }
        val toYaml = yaml.Printer(stringStyle = StringStyle.Literal).pretty(Json.fromJsonObject(toJson))
        file.overwrite(toYaml)
      }
    }
  }

}

