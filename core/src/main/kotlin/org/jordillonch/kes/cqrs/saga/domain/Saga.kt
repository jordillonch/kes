package org.jordillonch.kes.cqrs.saga.domain

import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

abstract class Saga(
    private val commandBus: CommandBus,
    private val eventBus: EventBus
) {

    init {
        registerCommandHandlers()
        registerEventHandlers()
    }

    private fun registerEventHandlers() {
        this.javaClass.kotlin
            .declaredFunctions
            .forEach { function ->
                function.parameters
                    .map { it.type.jvmErasure }
                    .firstOrNull { it.isSubclassOf(EventBus::class) }
                    .let { registerEventHandler(function) }
            }
    }

    private fun registerCommandHandlers() {
        this.javaClass.kotlin
            .declaredFunctions
            .forEach { function ->
                function.parameters
                    .map { it.type.jvmErasure }
                    .firstOrNull { it.isSubclassOf(Command::class) }
                    ?.let { registerCommandHandler(function) }
            }
    }

    private fun registerCommandHandler(handler: KFunction<*>) {
        val commandType = handler.parameters[1].type.jvmErasure.java
        commandBus.registerHandler(commandType) { command: Command -> handler.call(this, command) }
    }

    private fun registerEventHandler(handler: KFunction<*>) {
        val eventType = handler.parameters[1].type.jvmErasure.java
        eventBus.registerHandler(eventType) { event: Event -> handler.call(this, event) }
    }
}
