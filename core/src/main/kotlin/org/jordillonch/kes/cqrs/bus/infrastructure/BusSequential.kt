package org.jordillonch.kes.cqrs.bus.infrastructure

import org.jordillonch.kes.cqrs.bus.domain.*
import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.domain.entity.GenericRepository
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf

class BusSequential(associator: Associator, genericRepository: GenericRepository) : Bus(associator, genericRepository) {
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
