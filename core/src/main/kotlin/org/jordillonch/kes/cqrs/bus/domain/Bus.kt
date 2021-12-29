package org.jordillonch.kes.cqrs.bus.domain

import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandler
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandlerRepository
import org.jordillonch.kes.cqrs.bus.domain.entity.Repository
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

typealias Handler = (Effect) -> List<Effect>

abstract class Bus(private val associator: Associator) {
    private val handlers: MutableMap<String, MutableList<Handler>> = mutableMapOf()
    private val queue: ArrayDeque<Effect> = ArrayDeque()
    private val entityHandlerRepository = EntityHandlerRepository()

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

    fun <E, I> register(
        handler: KClass<out EntityHandler>,
        instanceCreator: () -> EntityHandler,
        repository: Repository<E, I>
    ) {
        handler.declaredFunctions
            .filter { function -> functionsWithEffectParameter(function) }
            .forEach { function ->
                registerHandlerWithEntity(
                    handler,
                    function,
                    instanceCreator,
                    repository
                )
            }
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
        function: KFunction<*>,
        entityInstanceCreator: () -> EntityHandler,
        repository: Repository<E, I>?
    ) {
        if (function.parameters.size == 2) {
            registerHandlerWithEntityConstructor(function, entityInstanceCreator)
        } else if (function.parameters.size == 3 && repository != null) {
            registerHandlerWithEntityFromRepository(function, handler, repository, entityInstanceCreator)
        } else {
            throw IllegalArgumentException("Invalid handler function")
        }
    }

    private fun registerHandlerWithEntityConstructor(
        function: KFunction<*>,
        entityInstanceCreator: () -> EntityHandler
    ) {
        val effectType = function.parameters[1].type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        handlers
            .getOrPut(effectType.java.canonicalName) { mutableListOf() }
            .add { effect: Effect -> function.call(entityInstanceCreator(), effect) as List<Effect> }
    }

    private fun <E, I> registerHandlerWithEntityFromRepository(
        function: KFunction<*>,
        handler: KClass<out EntityHandler>,
        repository: Repository<E, I>,
        entityInstanceCreator: () -> EntityHandler
    ) {
        val effectType = function.parameters[2].type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        handlers
            .getOrPut(effectType.java.canonicalName) { mutableListOf() }
            .add { effect: Effect ->
                associator
                    .entityIdsFor(handler, effect)
                    .map { entityId ->
                        val entity = repository.find(entityId as I)
                        if (entityFunctionParameterMatchesEntityType<E>(entity, function)) {
                            function.call(entityInstanceCreator(), entity, effect) as List<Effect>
                        } else {
                            emptyList()
                        }
                    }
                    .flatten()
            }
        entityHandlerRepository.register(repository, repository.entityType())
    }

    private fun functionsWithEffectParameter(function: KFunction<*>) =
        function.parameters.any { it.type.jvmErasure.isSubclassOf(Effect::class) }

    private fun <E> entityFunctionParameterMatchesEntityType(entity: E?, function: KFunction<*>) =
        entity!!::class == function.parameters[1].type.jvmErasure
}
