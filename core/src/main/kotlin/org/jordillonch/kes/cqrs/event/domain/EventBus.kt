package org.jordillonch.kes.cqrs.event.domain

import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.EffectsHandler

// TODO: add occurred on property
interface Event : Effect

interface EventsHandler<in E : Event>: EffectsHandler

interface EventBus {
    fun <E : Event> registerHandler(handler: EventsHandler<E>)
    fun <E : Event> registerHandler(eventType: Class<*>, handler: (E) -> Unit)
    fun publish(event: Event)
    fun publish(events: List<Event>)
}
