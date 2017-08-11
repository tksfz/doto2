package org.tksfz.doto.model

import java.time.Instant
import java.util.UUID

import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time._
import org.tksfz.doto.project.Keyable

case class WhoWhat(user: String, id: Id)
case class Status(createdBy: String, createdAt: Instant, updatedAt: Instant, expiresAt: Instant, message: String)

object Status {
  implicit val statusDecoder: Decoder[Status] = deriveDecoder
  implicit val statusEncoder: Encoder[Status]  = deriveEncoder
}

object WhoWhat {
  implicit val whoWhatKey: Keyable[WhoWhat] = new Keyable[WhoWhat] {
    override def toString(k: WhoWhat): String = k.user + "/" + k.id.toString

    override def fromString(s: String): WhoWhat = {
      val Array(who, what) = s.split('/')
      val id = UUID.fromString(what)
      WhoWhat(who, id)
    }
  }
}