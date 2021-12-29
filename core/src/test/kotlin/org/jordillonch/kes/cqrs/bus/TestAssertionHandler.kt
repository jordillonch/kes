package org.jordillonch.kes.cqrs.bus

import io.kotest.matchers.collections.shouldContain
import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.command.SimpleBusTest
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventsHandler
import kotlin.reflect.KClass

class TestAssertionHandler : EventsHandler<SimpleBusTest.ATestEvent> {
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
