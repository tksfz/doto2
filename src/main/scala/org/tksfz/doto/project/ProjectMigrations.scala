package org.tksfz.doto.project

import io.circe.{Json, JsonObject}
import org.tksfz.doto.model.{Event, Task, Thread}
import org.tksfz.doto.store.Migration

import scala.collection.SortedMap
import scala.reflect.{ClassTag, classTag}

case class Migrations(migrations: SortedMap[Int, PartialFunction[Class[_], Migration]]) {
  lazy val maxVersion = migrations.keys.max

  def forType[T : ClassTag]: SortedMap[Int, Migration] = {
    migrations
      .mapValues(_.lift(classTag[T].runtimeClass))
      .collect { case (version, Some(migration)) => version -> migration }
  }
}

object ProjectMigrations {
  def nodeVersion = nodeMigrations.maxVersion

  val nodeMigrations = Migrations(SortedMap(
    2 ->
      {
        case x if isClass[Thread[_], Task, Event](x) =>
          Migration(
            "Move node data under new `content` field",
            { json: JsonObject =>
              json
                .add("content",
                  Json.fromFields(
                    Seq(
                      json("subject").map("subject" -> _),
                      json("description").map("description" -> _),
                      json("completed").map("completed" -> _)
                    ).flatten
                  )
                )
                .remove("subject")
                .remove("description")
                .remove("completed")
            }
          )
      }
  ))

  private[this] def isClass[A : ClassTag](x: Class[_]) = {
    classTag[A].runtimeClass == x
  }

  private[this] def isClass[A : ClassTag, B : ClassTag](x: Class[_]) = {
    isClass[A](x) || isClass[B](x)
  }

  private[this] def isClass[A : ClassTag, B : ClassTag, C : ClassTag](x: Class[_]) = {
    isClass[A](x) || isClass[B](x) || isClass[C](x)
  }
}

trait ProjectMigrations {
  self: Project =>

  def checkAndRunMigrations() = {
    val nodeMigrations = ProjectMigrations.nodeMigrations
    val checkMigrations =
      Seq(threads.checkMigrations(nodeMigrations.forType[Thread[_]]),
        tasks.checkMigrations(nodeMigrations.forType[Task]),
        events.checkMigrations(nodeMigrations.forType[Event]))
    val neededMigrations = checkMigrations.flatMap(_.neededMigrations).distinct
    if (neededMigrations.nonEmpty) {
      println(s"Some documents may be out-of-date. Upgrading to schema version ${nodeMigrations.maxVersion}: " +
        s"${neededMigrations.map(_._2.message).mkString(" ")}")
      checkMigrations.foreach(_.run())
    }
  }


}

