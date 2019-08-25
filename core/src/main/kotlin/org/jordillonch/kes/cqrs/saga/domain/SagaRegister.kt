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
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

class SagaRegister(
    private val commandBus: CommandBus,
    private val eventBus: EventBus,
    private val sagaAssociationRepository: SagaAssociationRepository,
    private val sagaStateRepository: SagaStateRepository
) {

    private val handlersRegistered = mutableListOf<Triple<KClass<out Saga>, CommandBus, EventBus>>()

    fun associate(
        saga: Saga,
        effectKClass: KClass<out Effect>,
        associatedProperty: KProperty1<*, Any>,
        associatedPropertyValue: Any
    ) {
        sagaAssociationRepository.associate(
            saga.id,
            saga.name(),
            effectKClass,
            associatedProperty,
            associatedPropertyValue
        )
    }

    fun init(saga: Saga) {
        registerHandlersOncePerSagaClassAndBuses(saga)
    }

    private fun registerHandlersOncePerSagaClassAndBuses(saga: Saga) {
        val kClassRegisteredForCommandAndEventBus = Triple(saga.javaClass.kotlin, commandBus, eventBus)
        if (!handlersRegistered.contains(kClassRegisteredForCommandAndEventBus)) findAndRegisterHandlers(saga)
        handlersRegistered.add(kClassRegisteredForCommandAndEventBus)
    }

    private fun findAndRegisterHandlers(saga: Saga) {
        saga.javaClass.kotlin.declaredFunctions
            .filter { kFunction ->
                kFunction.parameters.any { it.type.jvmErasure.isSubclassOf(Effect::class) }
            }
            .forEach { registerHandler(saga, it) }
    }

    private fun registerHandler(saga: Saga, handler: KFunction<*>) {
        val effectType = handler.parameters[1].type.jvmErasure
        if (effectType.isSubclassOf(Command::class)) {
            commandBus.registerHandler(effectType.java, handler(saga, handler))
        } else if (effectType.isSubclassOf(Event::class)) {
            eventBus.registerHandler(effectType.java, handler(saga, handler))
        }
    }

    private fun handler(saga: Saga, handler: KFunction<*>): (Effect) -> Unit =
        { effect: Effect ->
            val shadowSagaInstance = recoverSagaState(saga, effect)
            handler.call(shadowSagaInstance, effect)
            saveSagaState(shadowSagaInstance)
        }

    private fun recoverSagaState(saga: Saga, effect: Effect): Saga {
        val sagaInstance = saga.newInstance()
        sagaAssociationRepository
            .find(saga.name(), effect)
            ?.let { sagaStateRepository.find(it) }
            ?.forEach { name, value ->
                sagaInstance.javaClass
                    .getDeclaredField(name)
                    .also { it.isAccessible = true }
                    .set(sagaInstance, value)
            }
        return sagaInstance
    }

    @Suppress("UNCHECKED_CAST")
    private fun saveSagaState(sagaInstance: Saga) {
        sagaInstance.javaClass.kotlin.memberProperties
            .associateBy { it.name }
            .map { (k, v) -> k to v.also { it.isAccessible = true }.get(sagaInstance) }
            .run { sagaStateRepository.save(sagaInstance.id, toMap() as Map<String, Any>) }
    }
}
