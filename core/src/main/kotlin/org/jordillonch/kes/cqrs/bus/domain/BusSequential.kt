package org.jordillonch.kes.cqrs.bus.domain

import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandler
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandlerRepository
import org.jordillonch.kes.cqrs.bus.domain.entity.Repository
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class BusSequential(private val associator: Associator) : Bus {
    private val handlers: MutableMap<String, MutableList<Handler>> = mutableMapOf()
    private val queue: ArrayDeque<Effect> = ArrayDeque()
    private val entityHandlerRepository = EntityHandlerRepository()

    init {
        register(entityHandlerRepository)
        register(associator.handler())
    }

    override fun register(handler: EffectsHandler) {
        handler.javaClass.kotlin.declaredFunctions
            .filter { function -> functionsWithEffectParameter(function) }
            .forEach { function -> registerHandler(handler, function) }
    }

    override fun <E, I> register(
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

    override fun push(effect: Effect) = queue.add(effect).let { }

    override fun push(effects: List<Effect>) = effects.forEach { push(it) }

    override fun drain() {
        while (queue.isNotEmpty()) {
            val effect = queue.removeFirst()
            val effectNameAndSuperclassNames =
                listOf(effect::class.qualifiedName) + effect::class.allSuperclasses.map { it.qualifiedName }
            effectNameAndSuperclassNames
                .map { handlers.getOrDefault(it!!, mutableListOf()) }.flatten()
                .also { guardCommandHasAHandler(effect, it) }
                .forEach { handler -> handler(effect).also(this::push) }
        }
    }

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

    private fun guardCommandHasAHandler(effect: Effect, handlersToExecute: List<Handler>) {
        if (effect::class.isSubclassOf(Command::class) && handlersToExecute.isEmpty()) {
            throw NoCommandHandlerFoundException(effect as Command)
        }
    }

    private fun <E> entityFunctionParameterMatchesEntityType(entity: E?, function: KFunction<*>) =
        entity!!::class == function.parameters[1].type.jvmErasure
}
