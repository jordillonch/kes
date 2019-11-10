package org.jordillonch.kes.cqrs.unit_of_work.domain

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.cqrs.unit_of_work.infrastructure.SimpleTransactionManager
import javax.transaction.Status
import javax.transaction.TransactionManager

class UoW(
    commandBus: CommandBus,
    eventBus: EventBus,
    effectQueue: EffectQueue,
    transactionManager: TransactionManager = SimpleTransactionManager()
) {
    private val uowBus = UoWBusCoordinator(transactionManager, commandBus, eventBus, effectQueue)
    val commandBus = UoWBusCommand(uowBus, commandBus)
    val eventBus = UoWBusEvent(uowBus, eventBus)
}

class UoWBusCoordinator(
    private var transactionManager: TransactionManager,
    private val commandBus: CommandBus,
    private val eventBus: EventBus,
    private val effectQueue: EffectQueue
) {

    fun handle(command: Command) {
        transactionManager.transaction
        if (transactionManager.status == Status.STATUS_PREPARED) {
            transactionManager.begin()
            commandBus.handle(command)
            transactionManager.commit()
        } else {
            effectQueue.add(command)
        }
        processQueuedEffects()
    }

    fun handle(event: Event) {
        transactionManager.transaction
        if (transactionManager.status == Status.STATUS_PREPARED) {
            transactionManager.begin()
            eventBus.publish(event)
            transactionManager.commit()
        } else {
            effectQueue.add(event)
        }
        processQueuedEffects()
    }

    private tailrec fun processQueuedEffects() {
        if (transactionManager.status == Status.STATUS_ACTIVE) return
        if (effectQueue.isEmpty()) return
        effectQueue.poll().run { processEffect(this) }
        processQueuedEffects()
    }

    private fun processEffect(effect: Effect) {
        transactionManager.begin()
        when (effect) {
            is Command -> commandBus.handle(effect)
            is Event -> eventBus.publish(effect)
        }
        transactionManager.commit()
    }
}

class UoWBusCommand(private val uowBusCoordinator: UoWBusCoordinator, commandBus: CommandBus) :
    CommandBus by commandBus {
    override fun handle(command: Command) {
        uowBusCoordinator.handle(command)
    }
}

class UoWBusEvent(private val uowBusCoordinator: UoWBusCoordinator, eventBus: EventBus) : EventBus by eventBus {
    override fun publish(event: Event) {
        uowBusCoordinator.handle(event)
    }

    override fun publish(events: List<Event>) {
        events.forEach { publish(it) }
    }
}
