package org.jordillonch.kes.event_sourcing.domain

import org.jordillonch.kes.cqrs.event.domain.Event

interface Aggregate {
    fun id(): AggregateId
    fun process(event: Event): Int
}