package org.jordillonch.kes.event_sourcing.domain

import org.jordillonch.kes.cqrs.event.domain.Event

object AggregateLifeCycle {
    private lateinit var eventStore: EventStore

    fun init(store: EventStore) {
        eventStore = store
    }

    fun applyEvent(aggregate: Aggregate, event: Event) {
        val eventSequence = aggregate.process(event)
        eventStore.append(aggregate, eventSequence, event)
    }
}