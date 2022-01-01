package org.jordillonch.kes.event_sourcing.integration

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationIdsRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationTypesRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.BusSequential
import org.jordillonch.kes.cqrs.bus.infrastructure.GenericRepositoryInMemory
import org.jordillonch.kes.event_sourcing.domain.AggregateLifeCycle
import org.jordillonch.kes.event_sourcing.infrastructure.InMemoryEventStoreWithPublishToEventBus
import org.jordillonch.kes.event_sourcing.stub.FooAggregate
import org.jordillonch.kes.event_sourcing.stub.FooCreatedEvent
import org.jordillonch.kes.event_sourcing.stub.FooId

class InMemoryEventStoreWithPublishToEventBusTest : ShouldSpec(
    {
        val associationIdsRepository = AssociationIdsRepositoryInMemory()
        val associationTypeRepository = AssociationTypesRepositoryInMemory()
        val associator = Associator(associationIdsRepository, associationTypeRepository)
        val genericRepository = GenericRepositoryInMemory()
        val bus = BusSequential(associator, genericRepository)
        val eventStore = InMemoryEventStoreWithPublishToEventBus(bus)
        AggregateLifeCycle.init(eventStore)

        beforeEach { bus.drain() }

        should("create an aggregate and publish event to the event bus") {
            val id = FooId.random()
            val event = FooCreatedEvent(id.id())
            val aggregate = FooAggregate.create(id)

            aggregate.id shouldBe id
            bus.events() shouldBe listOf(event)
        }

        should("load an aggregate") {
            val id = FooId.random()
            val event = FooCreatedEvent(id.id())
            val createdAggregate = FooAggregate.create(id)
            val loadedAggregate = eventStore.load(::FooAggregate, id)

            loadedAggregate.id shouldBe createdAggregate.id
            loadedAggregate.shouldBeTypeOf<FooAggregate>()
            bus.events() shouldBe listOf(event)
        }
    })
