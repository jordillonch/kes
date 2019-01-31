package org.jordillonch.kes.event_sourcing.integration

import io.kotlintest.specs.ShouldSpec
import org.jordillonch.kes.event_sourcing.domain.AggregateLifeCycle
import org.jordillonch.kes.event_sourcing.infrastructure.KafkaEventStore
import org.jordillonch.kes.event_sourcing.stub.FooAggregate
import org.jordillonch.kes.event_sourcing.stub.FooId

class KafkaEventStoreTest : ShouldSpec(
        {
            val eventStore = KafkaEventStore()
            AggregateLifeCycle.init(eventStore)

            should(" ") {
                val id = FooId.random()
                val aggregate = FooAggregate.create(id)
                Thread.sleep(1000)
                eventStore.load(::FooAggregate, id)
            }
        }
)