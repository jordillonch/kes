package org.jordillonch.kes.cqrs.event.domain

import org.jordillonch.kes.cqrs.Effect

// TODO: add occurred on property
interface Event : Effect

interface EventHandler<in E : Event> {
    fun on(event: E)
}

interface EventBus {
    fun <E : Event> registerHandler(handler: EventHandler<E>)
    fun <E : Event> registerHandler(handler: (E) -> Unit)
    fun <E : Event> registerHandler(eventType: Class<*>, handler: (E) -> Unit)
    fun publish(event: Event)
    fun publish(events: List<Event>)
}
