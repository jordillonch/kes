package org.jordillonch.kes.event_sourcing.infrastructure

import org.jordillonch.kes.cqrs.bus.domain.Event
import org.jordillonch.kes.event_sourcing.domain.Aggregate
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaType

// TODO: improve adding cache
abstract class AbstractAggregate : Aggregate {
    private var currentEventSequence = 1

    override fun process(event: Event): Int {
        javaClass.kotlin.declaredFunctions
            .first {
                it.name == "on" &&
                        it.parameters[1].type.javaType == event.javaClass
            }
            .call(this, event)

        return currentEventSequence++
    }
}
