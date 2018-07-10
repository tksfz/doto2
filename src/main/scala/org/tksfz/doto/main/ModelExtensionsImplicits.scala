package org.tksfz.doto.main

import org.tksfz.doto.model.HasContent

object ModelExtensionsImplicits {
  val Subject = "subject"
  val Completed = "completed"
  val Description = "description"

  implicit class HasContentExtensionMethods[T <: HasContent](val obj: T) extends AnyVal {
    def subject = obj.field[String](Subject).getOrElse("")
    def completed = obj.field[Boolean](Completed).getOrElse(false)
    def descriptionStr = obj.field[String](Description).getOrElse("")

    def withSubject(s: String) = obj.withContentField(Subject, s)

    def withCompleted(f: Boolean) = obj.withContentField(Completed, f)

    def withDescription(s: String) = obj.withContentField(Description, s)
  }
}
