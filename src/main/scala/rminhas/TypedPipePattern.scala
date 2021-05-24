package rminhas

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

// pipe pattern = forward the result of a future back to me as a message.

object TypedPipePattern {

  object Infrastructure {
    private implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
    private val db: Map[String, Int] = Map(
      "riz" -> 123,
      "wan" -> 456,
      "min" -> 789,
      "has" -> 159
    )

    def asyncGetPhoneNumber(name: String): Future[Int] =
      Future(db(name))
  }

  trait PhoneCallProtocol

  case class FindAndCall(name: String) extends PhoneCallProtocol

  case class InitiatePhoneCall(number: Int) extends PhoneCallProtocol

  case class LogPhoneCallFailure(reason: Throwable) extends PhoneCallProtocol

  // This breaks actor encapsulation and cause race conditions.
  // Also you can't use context.log.* as it would throw UnsupportedOperationException: Unsupported access to ActorContext from the outside of Actor
  val phoneCallInitiatorV1: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0
    implicit val ec: ExecutionContext = context.executionContext

    Behaviors.receiveMessage {
      case FindAndCall(name) =>
        val futureNumber: Future[Int] = Infrastructure.asyncGetPhoneNumber(name)
        futureNumber.onComplete {
          case Success(number) =>
            println(s"Initiating phone call for $number")
            nPhoneCalls += 1
          case Failure(exception) =>
            println(s"Phone call failed for $name: $exception")
            nFailures += 1
        }
        Behaviors.same
    }
  }

  val phoneCallInitiatorV2: Behavior[PhoneCallProtocol] = Behaviors.setup { context =>
    var nPhoneCalls = 0
    var nFailures = 0

    Behaviors.receiveMessage {
      case FindAndCall(name) =>
        val futureNumber: Future[Int] = Infrastructure.asyncGetPhoneNumber(name)
        context.pipeToSelf(futureNumber) {
          case Success(number) => InitiatePhoneCall(number)
          case Failure(ex) => LogPhoneCallFailure(ex)
        }
        Behaviors.same
      case InitiatePhoneCall(number) =>
        context.log.info(s"Initiating phone call for $number")
        nPhoneCalls += 1 // not a race condition
        Behaviors.same
      case LogPhoneCallFailure(ex) =>
        context.log.error(s"Phone call failed: $ex")
        nFailures += 1
        Behaviors.same
    }
  }

  def phoneCallInitiatorV3(nPhoneCalls: Int = 0, nFailures: Int = 0): Behavior[PhoneCallProtocol] =
    Behaviors.receive { (context, message) =>
      message match {
        case FindAndCall(name) =>
          val futureNumber: Future[Int] = Infrastructure.asyncGetPhoneNumber(name)
          context.pipeToSelf(futureNumber) {
            case Success(number) => InitiatePhoneCall(number)
            case Failure(ex) => LogPhoneCallFailure(ex)
          }
          Behaviors.same
        case InitiatePhoneCall(number) =>
          context.log.info(s"Initiating phone call for $number")
          phoneCallInitiatorV3(nPhoneCalls + 1, nFailures)
        case LogPhoneCallFailure(ex) =>
          context.log.error(s"Phone call failed: $ex")
          phoneCallInitiatorV3(nPhoneCalls, nFailures + 1)
      }
    }

  def main(args: Array[String]): Unit = {

    val root = ActorSystem(phoneCallInitiatorV3(), "PhoneCaller")

    root ! FindAndCall("riz")
    root ! FindAndCall("foo")

    Thread.sleep(1000)
    root.terminate()
  }

}
