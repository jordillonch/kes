package org.jordillonch.kes.event_sourcing.stub

import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.event_sourcing.domain.AggregateId
import org.jordillonch.kes.event_sourcing.domain.AggregateLifeCycle
import org.jordillonch.kes.event_sourcing.infrastructure.AbstractAggregate
import java.io.Serializable
import java.util.*

class FooAggregate : AbstractAggregate(), Serializable {

    lateinit var id: FooId private set

    override fun id() = id

    companion object {
        fun create(id: FooId) =
                FooAggregate()
                        .also {
                            AggregateLifeCycle.applyEvent(it, FooCreatedEvent(id.id()))
                        }
    }

    fun on(event: FooCreatedEvent) {
        id = FooId.fromString(event.id)
    }
}

data class FooId(val id: UUID) : AggregateId {
    companion object {
        fun fromString(id: String) = FooId(UUID.fromString(id))

        fun random() = FooId(UUID.randomUUID())
    }

    override fun id() = id.toString()
}

data class FooCreatedEvent(val id: String) : Event