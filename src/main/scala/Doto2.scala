/**
  * Created by thom on 3/8/17.
  */
object Doto2 {

  trait Ref[T]

  trait Item

  abstract class Item(
    id: Int,
    complete: Boolean // this typically is a denormalized thing
  )

  abstract class ContainsTasks(
    children: Seq[Ref[Task3]]
  )

  abstract class CanBePartOfEvent(
    partOf: Option[Ref[Event]]
  )

  // Thread extends ContainsTasks
  // Event extends CanBePartOfEvent
  // Task extends ContainsTasks with CanBePartOfEvent

  /**
    * It makes sense for parent to be here, since threads are spawned
    *
    * Threads are at the top levels, since Events and Tasks both require a Parent thread
    * @param id
    * @param parent
    */
  case class Thread(
    id: Int,
    parent: Option[Ref[Thread]],
    complete: Boolean, /* this might in future be expanded to gmail label-like system */
    children: Seq[Ref[Task3]]
  )

  case class Event(id: Int, parent: Ref[Thread], partOf: Option[Ref[Event]])

  case class Task(id: Int, parents: Seq[Ref[Either[Thread, Event]]])

  // or..

  case class Task2(id: Int, threadParent: Ref[Thread], eventParent: Ref[Event])


  case class Task3(id: Int, children: Seq[Ref[Task3]], event: Option[Ref[Event]])


  // how an object of type T should be displayed in "doto ls"
  trait Display[T] {
    def print(t: T): String
  }


  // constraints on the children.. type-level?
  // e.g. all children must be events? or sub-threads..
  // events can't have children?
  trait ConstrainChildren[T] {
    def isChildAllowed(t: Item)
  }

  // not sure if this is needed
  trait ChildDefaults[T] {

  }
}
