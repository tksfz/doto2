package org.tksfz.doto.model

import java.time.Instant

import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time._

case class Status(createdBy: String, createdAt: Instant, updatedAt: Instant, expiresAt: Instant, message: String)

object Status {
  implicit val statusDecoder: Decoder[Status] = deriveDecoder
  implicit val statusEncoder: Encoder[Status]  = deriveEncoder
}