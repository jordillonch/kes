package org.jordillonch.kes.event_sourcing.infrastructure

import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.event_sourcing.domain.Aggregate
import org.jordillonch.kes.event_sourcing.domain.AggregateId
import org.jordillonch.kes.event_sourcing.domain.AggregateAlreadyExistsException
import org.jordillonch.kes.event_sourcing.domain.EventStore
import java.util.concurrent.ConcurrentHashMap

// this implementation has coupled the event store and the event bus,
// it is possible to implement a event store that just write to a database and use other
// implementation to track in the infrastructure side new events in order to publish to an
// event bus
class InMemoryEventStoreWithPublishToEventBus(private val eventBus: EventBus) : EventStore {
    private val store: ConcurrentHashMap<AggregateId, MutableList<Event>> = ConcurrentHashMap()

    override fun <A : Aggregate> load(factory: () -> A, id: AggregateId): A {
        val events = store[id]
        return factory()
                .also { events!!.forEach { event -> it.process(event) } }
    }

    override fun append(aggregate: Aggregate, eventSequence: Int, event: Event) {
        store.compute(aggregate.id()) { _, eventsList ->
            guardAggregateExists(eventSequence, eventsList, aggregate)
            eventsList?.also { it.add(event) } ?: mutableListOf(event)
        }

        eventBus.publish(event)
    }

    private fun guardAggregateExists(eventSequence: Int,
                                     eventsList: MutableList<Event>?,
                                     aggregate: Aggregate) {
        if (eventSequence == 1 && eventsList != null)
            throw AggregateAlreadyExistsException.appendingFirstEvent(aggregate)
    }
}