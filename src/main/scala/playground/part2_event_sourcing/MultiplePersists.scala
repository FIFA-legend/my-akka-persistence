package playground.part2_event_sourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object MultiplePersists extends App {

  /*
    Diligent accountant: with every invoice, will persist TWO events
    - a tax record for the fiscal authority
    - an invoice record for personal logs or some auditing authority
   */

  // COMMAND
  case class Invoice(recipient: String, date: Date, amount: Int)

  // EVENTS
  case class TaxRecord(taxId: String, recordId: Int, date: Date, totalAmount: Int)
  case class InvoiceRecord(invoiceRecordId: Int, recipient: String, date: Date, amount: Int)

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef): Props = Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {

    var latestTaxRecordId = 0
    var latestInvoiceRecordId = 0

    override def persistenceId: String = "diligent-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        // journal ! TaxRecord
        persist(TaxRecord(taxId, latestTaxRecordId, date, amount / 3)) { record =>
          taxAuthority ! record
          latestTaxRecordId += 1

          persist("I hereby declare this tax record to be true and complete") { declaration =>
            taxAuthority ! declaration
          }
        }
        // journal ! InvoiceRecord
        persist(InvoiceRecord(latestInvoiceRecordId, recipient, date, amount)) { invoiceRecord =>
          taxAuthority ! invoiceRecord
          latestInvoiceRecordId += 1

          persist("I hereby declare this invoice record to be true and complete") { declaration =>
            taxAuthority ! declaration
          }
        }
    }

    override def receiveRecover: Receive = {
      case event => log.info(s"Recovered: $event")
    }
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Received: $message")
    }
  }

  val system = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant = system.actorOf(DiligentAccountant.props("UK52352_58325", taxAuthority))

  accountant ! Invoice("The Sofa Company", new Date, 2000)

  /*
    The message ordering (TaxRecord -> InvoiceRecord) is GUARANTEED.
    Journal is some sort of actor. Messages are sent to journal via "tell" method.
    So the order is guaranteed.
   */

  /**
    * PERSISTENCE IS ALSO BASED ON MESSAGE PASSING.
    */

  // inner "persist" call is executed after the outer "persist" call
  accountant ! Invoice("The Supercar Company", new Date, 20004302)

  /*
    Calls to persist() are executed IN ORDER
    Handlers for subsequent persist() calls are executed IN ORDER
   */

}
