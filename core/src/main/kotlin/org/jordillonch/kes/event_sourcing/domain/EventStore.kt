package org.jordillonch.kes.event_sourcing.domain

import org.jordillonch.kes.cqrs.bus.domain.Event


interface EventStore {
    fun <A : Aggregate> load(factory: () -> A, id: AggregateId): A
    fun append(aggregate: Aggregate, eventSequence: Int, event: Event)
}
