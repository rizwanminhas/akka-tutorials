package rminhas

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object TypedStatefulActors {

  trait MyTask

  case object TaskOne extends MyTask

  case object TaskTwo extends MyTask

  case object TaskThree extends MyTask

  def myTaskStatefulActor(): Behavior[MyTask] = Behaviors.setup { context =>
    var myState = 0
    Behaviors.receiveMessage {
      case TaskOne =>
        context.log.info(s"$myState | executing TaskOne")
        myState += 10
        Behaviors.same
      case TaskTwo =>
        context.log.info(s"$myState | executing TaskTwo")
        myState -= 20
        Behaviors.same
      case TaskThree =>
        context.log.info(s"$myState | executing TaskThree")
        myState += 50
        Behaviors.same
      case _ =>
        context.log.info(s"$myState | Unknown Task")
        Behaviors.same
    }
  }

  def main(args: Array[String]): Unit = {
    val myTaskActorSystem = ActorSystem(myTaskStatefulActor(), "MyTaskSystem")

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