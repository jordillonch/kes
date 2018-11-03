package org.jordillonch.kes.event_sourcing.behaviour

import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.jordillonch.kes.event_sourcing.domain.AggregateLifeCycle
import org.jordillonch.kes.event_sourcing.domain.EventStore
import org.jordillonch.kes.event_sourcing.stub.FooAggregate
import org.jordillonch.kes.event_sourcing.stub.FooCreatedEvent
import org.jordillonch.kes.event_sourcing.stub.FooId

class AbstractAggregateTest : ShouldSpec(
        {
            val eventStore: EventStore = mockk()
            AggregateLifeCycle.init(eventStore)

            should("create an aggregate") {
                val id = FooId.random()
                every { eventStore.append(ofType(FooAggregate::class), 1,
                                          FooCreatedEvent(id.id())) } just Runs
                val aggregate = FooAggregate.create(id)

                aggregate.id shouldBe id
                verify(exactly = 1) { eventStore.append(aggregate, 1,
                                                        FooCreatedEvent(id.id())) }
            }
        })