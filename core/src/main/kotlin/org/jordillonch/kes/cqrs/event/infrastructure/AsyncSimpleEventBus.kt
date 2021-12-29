package org.jordillonch.kes.cqrs.event.infrastructure

import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.cqrs.event.domain.EventsHandler
import java.util.concurrent.Executors

class AsyncSimpleEventBus(poolSize: Int = 4) : EventBus {
    private val simpleEventBus = SimpleEventBus()
    private val executor = Executors.newFixedThreadPool(poolSize)

    override fun <C : Event> registerHandler(handler: EventsHandler<C>) {
        simpleEventBus.registerHandler(handler)
    }

    fun <E : Event> registerHandler(handler: (E) -> Unit) {
        simpleEventBus.registerHandler(handler)
    }

    override fun <E : Event> registerHandler(eventType: Class<*>, handler: (E) -> Unit) {
        simpleEventBus.registerHandler(eventType, handler)
    }

    override fun publish(event: Event) {
        executor.submit {
            simpleEventBus.publish(event)
        }
    }

    override fun publish(events: List<Event>) {
        simpleEventBus.publish(events)
    }
}
