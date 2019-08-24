package org.jordillonch.kes.cqrs.saga.domain

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

abstract class Saga(
    private val commandBus: CommandBus,
    private val eventBus: EventBus,
    private val sagaAssociationRepository: SagaAssociationRepository,
    private val sagaStateRepository: SagaStateRepository
) {
    private val id = SagaId.new()

    init {
        registerHandlers()
    }

    abstract fun name(): String

    fun associate(
        effectKClass: KClass<out Effect>,
        associatedProperty: KProperty1<*, Any>,
        associatedPropertyValue: Any
    ) {
        sagaAssociationRepository.associate(id, name(), effectKClass, associatedProperty, associatedPropertyValue)
    }

    private fun registerHandlers() {
        this.javaClass.kotlin.declaredFunctions
            .filter { kFunction ->
                kFunction.parameters.any { it.type.jvmErasure.isSubclassOf(Effect::class) }
            }
            .forEach { registerHandler(it) }
    }

    private fun registerHandler(handler: KFunction<*>) {
        val effectType = handler.parameters[1].type.jvmErasure
        if (effectType.isSubclassOf(Command::class)) {
            commandBus.registerHandler(effectType.java, handler(handler))
        } else if (effectType.isSubclassOf(Event::class)) {
            eventBus.registerHandler(effectType.java, handler(handler))
        }
    }

    private fun handler(handler: KFunction<*>): (Effect) -> Unit =
        { effect: Effect ->
            recoverSagaState(effect)
            handler.call(this, effect)
            saveSagaState()
        }

    private fun recoverSagaState(effect: Effect) {
        sagaAssociationRepository
            .find(name(), effect)
            ?.let { sagaStateRepository.find(it) }
            ?.forEach { name, value ->
                this.javaClass.getDeclaredField(name)
                    .also { it.isAccessible = true }
                    .set(this, value)
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun saveSagaState() {
        this.javaClass.kotlin.memberProperties
            .associateBy { it.name }
            .map { (k, v) -> k to v.get(this) }
            .run { sagaStateRepository.save(id, toMap() as Map<String, Any>) }
    }
}
