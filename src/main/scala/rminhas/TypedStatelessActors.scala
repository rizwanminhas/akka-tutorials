package rminhas

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object TypedStatelessActors {

  trait MyTask

  case object TaskOne extends MyTask

  case object TaskTwo extends MyTask

  case object TaskThree extends MyTask

  def myTaskStatelessActor(myState: Int = 0): Behavior[MyTask] = Behaviors.receive { (context, message) =>
    message match {
      case TaskOne =>
        context.log.info(s"$myState | executing TaskOne")
        myTaskStatelessActor(myState + 10)
      case TaskTwo =>
        context.log.info(s"$myState | executing TaskTwo")
        myTaskStatelessActor(myState - 20)
      case TaskThree =>
        context.log.info(s"$myState | executing TaskThree")
        myTaskStatelessActor(myState + 50)
      case _ =>
        context.log.info(s"$myState | Unknown Task")
        Behaviors.same
    }
  }

  def main(args: Array[String]): Unit = {
    val myTaskActorSystem = ActorSystem(myTaskStatelessActor(), "MyTaskSystem")

    myTaskActorSystem ! TaskOne
    myTaskActorSystem ! TaskOne
    myTaskActorSystem ! TaskOne
    myTaskActorSystem ! TaskThree
    myTaskActorSystem ! TaskTwo
    myTaskActorSystem ! TaskTwo

    Thread.sleep(1000)
    myTaskActorSystem.terminate()
  }
}
