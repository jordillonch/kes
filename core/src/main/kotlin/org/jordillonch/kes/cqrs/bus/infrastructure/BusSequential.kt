package org.jordillonch.kes.cqrs.bus.infrastructure

import org.jordillonch.kes.cqrs.bus.domain.Bus
import org.jordillonch.kes.cqrs.bus.domain.Command
import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.Handler
import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf

class BusSequential(associator: Associator) : Bus(associator) {
    override fun drain() {
        while (!isQueueEmpty()) {
            val effect = removeFirstQueueElement()
            val effectNameAndSuperclassNames =
                listOf(effect::class.qualifiedName) + effect::class.allSuperclasses.map { it.qualifiedName }
            effectNameAndSuperclassNames
                .map { handlers(it!!) }.flatten()
                .also { guardCommandHasAHandler(effect, it) }
                .forEach { handler -> handler(effect).also(this::push) }
        }
    }

    private fun guardCommandHasAHandler(effect: Effect, handlersToExecute: List<Handler>) {
        if (effect::class.isSubclassOf(Command::class) && handlersToExecute.isEmpty()) {
            throw NoCommandHandlerFoundException(effect as Command)
        }
    }
}
