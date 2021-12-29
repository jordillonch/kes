package org.jordillonch.kes.cqrs.bus.infrastructure

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.bus.domain.Associator
import org.jordillonch.kes.cqrs.bus.domain.EntityHandler
import org.jordillonch.kes.cqrs.bus.domain.EntityHandlerRepository
import org.jordillonch.kes.cqrs.bus.domain.Repository
import org.jordillonch.kes.cqrs.command.domain.Bus
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.EffectHandler
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class BusSequential(private val associator: Associator) : Bus {
    private val handlers: MutableMap<String, MutableList<(Effect) -> List<Effect>>> = mutableMapOf()
    private val queue: ArrayDeque<Effect> = ArrayDeque()
    private val entityHandlerRepository = EntityHandlerRepository()

    init {
        register(entityHandlerRepository)
        register(associator.handler())
    }

    override fun register(handler: EffectHandler) {
        @Suppress("UNCHECKED_CAST")
        handler.javaClass.kotlin.declaredFunctions
            .filter { kFunction ->
                kFunction.parameters.any { it.type.jvmErasure.isSubclassOf(Effect::class) }
            }
            .forEach { registerHandler(handler, it) }
    }

    fun <E, I> register(
        handler: KClass<out EntityHandler>,
        instanceCreator: () -> EntityHandler,
        repository: Repository<E, I>
    ) {
        handler.declaredFunctions
            .filter { kFunction ->
                kFunction.parameters.any { it.type.jvmErasure.isSubclassOf(Effect::class) }
            }
            .forEach { handlerFunction ->
                registerHandler(handler, instanceCreator, handlerFunction, repository)
            }
    }

    override fun push(effect: Effect) {
        queue.add(effect)
    }

    override fun push(effects: List<Effect>) {
        effects.forEach { push(it) }
    }

    override fun drain() {
        while (queue.isNotEmpty()) {
            val effect = queue.removeFirst()
            val effectQualifiedNames =
                listOf(effect::class.qualifiedName) + effect::class.allSuperclasses.map { it.qualifiedName }
            val handlersToExecute = effectQualifiedNames.map { handlers.getOrDefault(it!!, mutableListOf()) }.flatten()
            handlersToExecute.forEach {
                it(effect)
                    .also(this::push)
            }
            // TODO: check this
            if (effect is Command && handlersToExecute == null) {
                throw NoCommandHandlerFoundException(effect)
            }
        }
    }

    private fun registerHandler(handler: EffectHandler, function: KFunction<*>) {
        val effectType = function.parameters[1].type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        handlers
            .getOrPut(effectType.java.canonicalName) { mutableListOf() }
            .add { effect: Effect ->
                function.call(handler, effect) as List<Effect>
            }
    }

    private fun <E, I> registerHandler(
        handler: KClass<out EntityHandler>,
        instanceCreator: () -> EntityHandler,
        handlerFunction: KFunction<*>,
        repository: Repository<E, I>
    ) {
        if (handlerFunction.parameters.size == 2) {
            // constructor handler
            val effectType = handlerFunction.parameters[1].type.jvmErasure
            @Suppress("UNCHECKED_CAST")
            handlers
                .getOrPut(effectType.java.canonicalName) { mutableListOf() }
                .add { effect: Effect ->
                    handlerFunction.call(instanceCreator(), effect) as List<Effect>
                }
        } else if (handlerFunction.parameters.size == 3) {
            // handler with entity
            val effectType = handlerFunction.parameters[2].type.jvmErasure
            @Suppress("UNCHECKED_CAST")
            handlers
                .getOrPut(effectType.java.canonicalName) { mutableListOf() }
                .add { effect: Effect ->
                    associator.entityIdsFor(handler, effect)
                        .map { entityId ->
                            val entity = repository.find(entityId as I)
                            // ensure handlerFunction matches with entity type
                            if (entity!!::class == handlerFunction.parameters[1].type.jvmErasure) {
                                handlerFunction.call(instanceCreator(), entity, effect) as List<Effect>
                            } else {
                                emptyList()
                            }
                        }
                        .flatten()
                }
            entityHandlerRepository.register(repository, repository.entityType())
        } else {
            throw IllegalArgumentException("Invalid handler function")
        }
    }
}

