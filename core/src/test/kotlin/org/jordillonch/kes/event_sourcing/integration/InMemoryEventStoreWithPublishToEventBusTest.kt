package org.jordillonch.kes.event_sourcing.integration

import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.event_sourcing.domain.AggregateLifeCycle
import org.jordillonch.kes.event_sourcing.infrastructure.InMemoryEventStoreWithPublishToEventBus
import org.jordillonch.kes.event_sourcing.stub.FooAggregate
import org.jordillonch.kes.event_sourcing.stub.FooCreatedEvent
import org.jordillonch.kes.event_sourcing.stub.FooId

class InMemoryEventStoreWithPublishToEventBusTest : ShouldSpec(
        {
            val eventBus: EventBus = mockk()
            val eventStore = InMemoryEventStoreWithPublishToEventBus(eventBus)
            AggregateLifeCycle.init(eventStore)

            should("create an aggregate and publish event to the event bus") {
                val id = FooId.random()
                val event = FooCreatedEvent(id.id())
                every { eventBus.publish(event) } just Runs
                val aggregate = FooAggregate.create(id)

                aggregate.id shouldBe id
                verify(exactly = 1) { eventBus.publish(event) }
            }

            should("load an aggregate") {
                val id = FooId.random()
                val event = FooCreatedEvent(id.id())
                every { eventBus.publish(event) } just Runs
                val createdAggregate = FooAggregate.create(id)
                val loadedAggregate = eventStore.load(::FooAggregate, id)

                loadedAggregate.id shouldBe createdAggregate.id
                loadedAggregate.shouldBeTypeOf<FooAggregate>()
            }
        })