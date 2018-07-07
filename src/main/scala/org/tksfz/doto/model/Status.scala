package org.tksfz.doto.model

import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.UUID

import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time._
import org.tksfz.doto.store.{HasKey, Key}

case class User(name: String, email: String)
case class WhoWhat(email: String, id: Id)
case class Status(createdBy: String, createdAt: Instant, updatedAt: Instant, expiresAt: Instant, user: User, nodeId: Id, message: String)

object Status {
  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val userEncoder: Encoder[User] = deriveEncoder
  implicit val statusDecoder: Decoder[Status] = deriveDecoder
  implicit val statusEncoder: Encoder[Status]  = deriveEncoder

  implicit val statusHasKey: HasKey[Status, WhoWhat] = new HasKey[Status, WhoWhat] {
    override def key(t: Status): WhoWhat = WhoWhat(t.user.email, t.nodeId)
  }
}

object WhoWhat {
  implicit val whoWhatKey: Key[WhoWhat] = new Key[WhoWhat] {
    override def toPath(k: WhoWhat): Path = Paths.get(k.email, k.id.toString)

    override def fromPathString(s: String): WhoWhat = {
      val Array(who, what) = s.split('/')
      val id = UUID.fromString(what)
      WhoWhat(who, id)
    }
  }
}