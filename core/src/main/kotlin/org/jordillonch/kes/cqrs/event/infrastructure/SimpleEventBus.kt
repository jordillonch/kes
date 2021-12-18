package org.jordillonch.kes.cqrs.event.infrastructure

import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.cqrs.event.domain.EventHandler
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

class SimpleEventBus : EventBus {
    private val handlers: MutableMap<String, MutableList<(Event) -> Unit>> = mutableMapOf()

    override fun <E : Event> registerHandler(handler: EventHandler<E>) {
        @Suppress("UNCHECKED_CAST")
        handlers.getOrPut(classFrom(handler)) { mutableListOf() }
            .add { event: Event -> handler.on(event as E) }
    }

    override fun <E : Event> registerHandler(handler: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        handlers.getOrPut(classFrom(handler)) { mutableListOf() }
            .add(handler as (Event) -> Unit)
    }

    override fun <E : Event> registerHandler(eventType: Class<*>, handler: (E) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        handlers.getOrPut(eventType.canonicalName) { mutableListOf() }
            .add(handler as (Event) -> Unit)
    }

    override fun publish(event: Event) {
        @Suppress("UNCHECKED_CAST")
        handlers[event::class.qualifiedName]
            ?.forEach { it.invoke(event) }
    }

    override fun publish(events: List<Event>) {
        events.forEach { publish(it) }
    }

    private fun <E : Event> classFrom(handler: (E) -> Unit) =
        handler.reflect()!!.parameters.first().type.toString()

    private fun <E : Event> classFrom(handler: EventHandler<E>) =
        handler.javaClass.kotlin
            .declaredFunctions
            .firstFunctionNamedOn()
            .mapParameterTypes()
            .first { it.isSubclassOf(Event::class) }
            .qualifiedName!!

    private fun Collection<KFunction<*>>.firstFunctionNamedOn() = first { it.name == "on" }

    private fun KFunction<*>.mapParameterTypes() = parameters.map { it.type.jvmErasure }
}
