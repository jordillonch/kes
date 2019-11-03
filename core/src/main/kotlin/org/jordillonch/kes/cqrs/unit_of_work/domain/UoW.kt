package org.jordillonch.kes.cqrs.unit_of_work.domain

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus

class UoW(commandBus: CommandBus, eventBus: EventBus, effectQueue: EffectQueue) {
    private val transactionManager = TransactionManager()
    private val uowBus = UoWBuses(transactionManager, commandBus, eventBus, effectQueue)
    val commandBus = UoWBusCommand(uowBus, commandBus)
    val eventBus = UoWBusEvent(uowBus, eventBus)
}

class UoWBuses(
    private var transactionManager: TransactionManager,
    private val commandBus: CommandBus,
    private val eventBus: EventBus,
    private val effectQueue: EffectQueue
) {

    fun handle(command: Command) {
        if (transactionManager.beginTransaction()) {
            commandBus.handle(command)
            transactionManager.commit()
        } else {
            effectQueue.add(command)
        }
        processQueuedEffects()
    }

    fun handle(event: Event) {
        if (transactionManager.beginTransaction()) {
            eventBus.publish(event)
            transactionManager.commit()
        } else {
            effectQueue.add(event)
        }
        processQueuedEffects()
    }

    private tailrec fun processQueuedEffects() {
        if (transactionManager.isBusy()) return
        if (effectQueue.isEmpty()) return
        effectQueue.poll().run { processEffect(this) }
        processQueuedEffects()
    }

    private fun processEffect(effect: Effect) {
        if (transactionManager.beginTransaction()) {
            when (effect) {
                is Command -> commandBus.handle(effect)
                is Event -> eventBus.publish(effect)
            }
            transactionManager.commit()
        } else throw RuntimeException("Transaction manager should be FREE")
    }
}

class UoWBusCommand(private val uowBuses: UoWBuses, commandBus: CommandBus) : CommandBus by commandBus {
    override fun handle(command: Command) {
        uowBuses.handle(command)
    }
}

class UoWBusEvent(private val uowBuses: UoWBuses, eventBus: EventBus) : EventBus by eventBus {
    override fun publish(event: Event) {
        uowBuses.handle(event)
    }

    override fun publish(events: List<Event>) {
        events.forEach { publish(it) }
    }
}
