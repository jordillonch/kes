package org.jordillonch.kes.event_sourcing.integration

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.*
import org.jordillonch.kes.cqrs.bus.domain.Bus
import org.jordillonch.kes.event_sourcing.domain.AggregateLifeCycle
import org.jordillonch.kes.event_sourcing.infrastructure.InMemoryEventStoreWithPublishToEventBus
import org.jordillonch.kes.event_sourcing.stub.FooAggregate
import org.jordillonch.kes.event_sourcing.stub.FooCreatedEvent
import org.jordillonch.kes.event_sourcing.stub.FooId

class InMemoryEventStoreWithPublishToEventBusTest : ShouldSpec(
    {
        val bus: Bus = mockk()
        val eventStore = InMemoryEventStoreWithPublishToEventBus(bus)
        AggregateLifeCycle.init(eventStore)

        should("create an aggregate and publish event to the event bus") {
            val id = FooId.random()
            val event = FooCreatedEvent(id.id())
            every { bus.push(event) } just Runs
            val aggregate = FooAggregate.create(id)

            aggregate.id shouldBe id
            verify(exactly = 1) { bus.push(event) }
        }

        should("load an aggregate") {
            val id = FooId.random()
            val event = FooCreatedEvent(id.id())
            every { bus.push(event) } just Runs
            val createdAggregate = FooAggregate.create(id)
            val loadedAggregate = eventStore.load(::FooAggregate, id)

            loadedAggregate.id shouldBe createdAggregate.id
            loadedAggregate.shouldBeTypeOf<FooAggregate>()
        }
    })
