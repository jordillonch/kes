package org.jordillonch.kes.cqrs.event.infrastructure

import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.cqrs.event.domain.EventHandler
import java.util.concurrent.Executors

class AsyncSimpleEventBus(poolSize: Int = 4) : EventBus {
    private val simpleEventBus = SimpleEventBus()
    private val executor = Executors.newFixedThreadPool(poolSize)

    override fun <C : Event> registerHandler(handler: EventHandler<C>) {
        simpleEventBus.registerHandler(handler)
    }

    override fun <E : Event> registerHandler(handler: (E) -> Unit) {
        simpleEventBus.registerHandler(handler)
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
