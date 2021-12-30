package org.jordillonch.kes.cqrs.bus

import io.kotest.matchers.collections.shouldContain
import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.Event
import kotlin.reflect.KClass

class TestAssertionHandler {
    private val events: MutableList<Event> = mutableListOf()

    fun on(event: Event): List<Effect> {
        events.add(event)
        return emptyList()
    }

    fun getEvents(): List<Event> = events

    infix fun shouldContainEvent(kClass: KClass<out Event>) {
        getEvents().map { it.javaClass.kotlin } shouldContain kClass
    }
}
