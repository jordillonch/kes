package org.jordillonch.kes.cqrs.event.domain

// TODO: add occurred on property
interface Event

interface EventHandler<in E : Event> {
    fun on(event: E)
}

interface EventBus {
    fun <E : Event> registerHandler(handler: EventHandler<E>)
    fun publish(event: Event)
    fun publish(events: List<Event>)
}