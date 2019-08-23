package org.jordillonch.kes.cqrs.saga.domain

import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

abstract class Saga(
    private val commandBus: CommandBus,
    private val eventBus: EventBus,
    private val sagaStateRepository: SagaStateRepository
) {
    private val id: UUID = UUID.randomUUID()

    init {
        registerCommandHandlers()
        registerEventHandlers()
    }

    abstract fun name(): String

    fun associate(effectKClass: KClass<*>, associatedPropertyName: String, associatedPropertyValue: UUID) {
        sagaStateRepository.associate(id, name(), effectKClass, associatedPropertyName, associatedPropertyValue)
    }

    private fun registerEventHandlers() {
        this.javaClass.kotlin.declaredFunctions
            .filter { kFunction ->
                kFunction.parameters.any { it.type.jvmErasure.isSubclassOf(Event::class) } }
            .forEach { registerEventHandler(it) }
    }

    private fun registerCommandHandlers() {
        this.javaClass.kotlin.declaredFunctions
            .filter { kFunction ->
                kFunction.parameters.any { it.type.jvmErasure.isSubclassOf(Command::class) } }
            .forEach { registerCommandHandler(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerCommandHandler(handler: KFunction<*>) {
        val commandType = handler.parameters[1].type.jvmErasure.java
        commandBus.registerHandler(commandType) { command: Command ->
            // handler function

            // get saga state from repository
            val state = sagaStateRepository.find(name(), command)
            // set state to current saga
            state?.forEach { name, value ->
                this.javaClass.getDeclaredField(name)
                    .also { it.isAccessible = true }
                    .set(this, value) }

            // call handler
            handler.call(this, command)

            // get current saga state
            val newState = this.javaClass.kotlin.memberProperties
                .associateBy { it.name }
                .map { (k, v) -> k to v.get(this) }
                .toMap() as Map<String, Any>
            // save current saga state
            sagaStateRepository.save(id, newState)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerEventHandler(handler: KFunction<*>) {
        val eventType = handler.parameters[1].type.jvmErasure.java
        eventBus.registerHandler(eventType) { event: Event ->
            // handler function

            // get saga state from repository
            val state = sagaStateRepository.find(name(), event)
            // set state to current saga
            state?.forEach { name, value ->
                this.javaClass.getDeclaredField(name)
                    .also { it.isAccessible = true }
                    .set(this, value) }

            // call handler
            handler.call(this, event)

            // get current saga state
            val newState = this.javaClass.kotlin.memberProperties
                .associateBy { it.name }
                .map { (k, v) -> k to v.get(this) }
                .toMap() as Map<String, Any>
            // save current saga state
            sagaStateRepository.save(id, newState)
        }
    }
}
