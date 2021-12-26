package org.jordillonch.kes.cqrs.command.infrastructure

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.command.domain.Bus
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.EffectHandler
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class SimpleBus() : Bus {
    private val handlers: MutableMap<String, MutableList<(Effect) -> List<Effect>>> = mutableMapOf()
    private val queue: ArrayDeque<Effect> = ArrayDeque()

    override fun register(handler: EffectHandler) {
        @Suppress("UNCHECKED_CAST")
        handler.javaClass.kotlin.declaredFunctions
            .filter { kFunction ->
                kFunction.parameters.any { it.type.jvmErasure.isSubclassOf(Effect::class) }
            }
            .forEach { registerHandler(handler, it) }
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
            val handlersToExecute = handlers[effect::class.qualifiedName]
            handlersToExecute?.forEach {
                it(effect).also(this::push)
            }
            if (effect is Command && handlersToExecute == null) {
                throw NoCommandHandlerFoundException(effect)
            }
        }
    }

    private fun registerHandler(handler: EffectHandler, function: KFunction<*>) {
        val effectType = function.parameters[1].type.jvmErasure
        @Suppress("UNCHECKED_CAST")
        handlers.getOrPut(effectType.java.canonicalName) { mutableListOf() }
            .add { effect: Effect -> function.call(handler, effect) as List<Effect> }
    }
}
