package org.jordillonch.kes.cqrs.bus.domain

import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandler
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandlerRepository
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityTypedRepository
import org.jordillonch.kes.cqrs.bus.domain.entity.GenericRepository
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

typealias Handler = (Effect) -> List<Effect>

abstract class Bus(
    private val associator: Associator,
    private val genericRepository: GenericRepository
) {
    private val handlers: MutableMap<String, MutableList<Handler>> = mutableMapOf()
    private val queue: ArrayDeque<Effect> = ArrayDeque()
    private val entityHandlerRepository = EntityHandlerRepository(genericRepository)

    init {
        register(entityHandlerRepository)
        register(associator.handler())
    }

    abstract fun drain()

    fun isQueueEmpty() = queue.isEmpty()

    fun removeFirstQueueElement() = queue.removeFirst()

    fun handlers(effectName: String) = handlers.getOrDefault(effectName, mutableListOf())

    fun register(handler: EffectsHandler) {
        handler.javaClass.kotlin.declaredFunctions
            .filter { function -> functionsWithEffectParameter(function) }
            .forEach { function -> registerHandler(handler, function) }
    }

    fun <E, I> register(instanceHandler: () -> EntityHandler, repository: EntityTypedRepository<E, I>?) {
        val handler = instanceHandler().javaClass.kotlin
        handler.declaredFunctions
            .filter { function -> functionsWithEffectParameter(function) }
            .forEach { function -> registerHandlerWithEntity(handler, instanceHandler, function, repository) }
    }

    fun register(instanceHandler: () -> EntityHandler) {
        register<Any, Any>(instanceHandler, null)
    }

    fun push(effect: Effect) = queue.add(effect).let { }

    fun push(effects: List<Effect>) = effects.forEach { push(it) }

    private fun registerHandler(handler: EffectsHandler, function: KFunction<*>) {
        val effectType = function.parameters[1].type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        handlers
            .getOrPut(effectType.java.canonicalName) { mutableListOf() }
            .add { effect: Effect -> function.call(handler, effect) as List<Effect> }
    }

    private fun <E, I> registerHandlerWithEntity(
        handler: KClass<out EntityHandler>,
        handlerInstance: () -> EntityHandler,
        function: KFunction<*>,
        repository: EntityTypedRepository<E, I>?
    ) {
        when (function.parameters.size) {
            2 -> registerHandlerWithEntityConstructor(handlerInstance, function)
            3 -> registerHandlerWithEntityFromRepository(handlerInstance, handler, function, repository)
            else -> throw IllegalArgumentException("Invalid handler function")
        }
    }

    private fun registerHandlerWithEntityConstructor(handlerInstance: () -> EntityHandler, function: KFunction<*>) {
        val effectType = function.parameters[1].type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        handlers
            .getOrPut(effectType.java.canonicalName) { mutableListOf() }
            .add { effect: Effect -> function.call(handlerInstance(), effect) as List<Effect> }
    }

    private fun <E, I> registerHandlerWithEntityFromRepository(
        handlerInstance: () -> EntityHandler,
        handler: KClass<out EntityHandler>,
        function: KFunction<*>,
        repository: EntityTypedRepository<E, I>?
    ) {
        val effectType = function.parameters[2].type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        handlers
            .getOrPut(effectType.java.canonicalName) { mutableListOf() }
            .add { effect: Effect ->
                associator
                    .entityIdsFor(handler, effect)
                    .map { entityId ->
                        val entity = repository?.find(entityId as I) ?: genericRepository.find(entityId)
                        if (entityFunctionParameterTypeMatchesEntityType(entity!!, function)) {
                            function.call(handlerInstance(), entity, effect) as List<Effect>
                        } else {
                            emptyList()
                        }
                    }
                    .flatten()
            }
        if (repository != null) entityHandlerRepository.register(repository, repository.entityType())
    }

    private fun functionsWithEffectParameter(function: KFunction<*>) =
        function.parameters.any { it.type.jvmErasure.isSubclassOf(Effect::class) }

    private fun entityFunctionParameterTypeMatchesEntityType(entity: Any, function: KFunction<*>) =
        entity::class == function.parameters[1].type.jvmErasure
}

class NoCommandHandlerFoundException(val command: Command) : RuntimeException()
