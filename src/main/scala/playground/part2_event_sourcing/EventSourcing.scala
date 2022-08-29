package playground.part2_event_sourcing

object EventSourcing {

  /*
    Pros:
    - high performance: events are only appended
    - avoids relational stores and ORM entirely
    - full trace of every state
    - fits tha Akka actor model perfectly

    Cons:
    - querying a state potentially expensive (Akka Persistence Query)
    - potential performance issues with long-lived entities (snapshotting)
    - data model subject to change (schema evolution)
    - just a very different model

    Extra capabilities of Persistent Actor:
    - have a persistent ID
    - persist events to a long-term store
    - recover state by replaying events from the store

    Message = Command
    Persistent store = journal
   */

}
