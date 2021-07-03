package rminhas

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object FireAndForgetDemo extends App {

  object Printer {
    case class PrintMe(message: String)

    def apply(): Behavior[PrintMe] =
      Behaviors.receive {
        case (context, PrintMe(message)) =>
          context.log.info(message)
          Behaviors.same
      }
  }

  val system = ActorSystem(Printer(), "fire-and-forget-sample")

  // note how the system is also the top level actor ref
  val printer: ActorRef[Printer.PrintMe] = system

  // these are all fire and forget
  printer ! Printer.PrintMe("message 1")
  printer ! Printer.PrintMe("not message 2")
}
