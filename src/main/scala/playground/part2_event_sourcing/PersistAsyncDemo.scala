package playground.part2_event_sourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistAsyncDemo extends App {

  case class Command(contents: String)
  case class Event(contents: String)

  object CriticalStreamProcessor {
    def props(eventAggregator: ActorRef): Props = Props(new CriticalStreamProcessor(eventAggregator))
  }

  class CriticalStreamProcessor(eventAggregator: ActorRef) extends PersistentActor with ActorLogging {
    override def persistenceId: String = "critical-stream-processor"

    override def receiveCommand: Receive = {
      case Command(contents) =>
        eventAggregator ! s"Processing $contents"
        // mutate
        persistAsync(Event(contents)) /* TIME GAP */ { e =>
          eventAggregator ! e
          // mutate
        }

        // some actual computation
        val processedContents = contents + "_processed"
        persistAsync(Event(processedContents)) { e =>
          eventAggregator ! e
        }
    }

    override def receiveRecover: Receive = {
      case message => log.info(s"Recovered: $message")
    }
  }

  class EventAggregator extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"$message")
    }
  }

  val system = ActorSystem("PersistAsyncDemo")
  val eventAggregator = system.actorOf(Props[EventAggregator], "eventAggregator")
  val streamProcessor = system.actorOf(CriticalStreamProcessor.props(eventAggregator), "streamProcessor")

  streamProcessor ! Command("Command1")
  streamProcessor ! Command("Command2")

  /*
    persistAsync vs persist
    - performance: high-throughput environment

    persist vs persistAsync
    - ordering guarantees
    - mutation of the state
   */

  /*
    persistAsync guarantees:
    - persist calls happen in order
    - persist callbacks are called in order
    - no other guarantees: new messages may be handled in the time gaps
   */

}
