package org.jordillonch.kes.cqrs.unit_of_work

import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandHandler
import org.jordillonch.kes.cqrs.command.infrastructure.SimpleCommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.cqrs.event.domain.EventHandler
import org.jordillonch.kes.cqrs.event.infrastructure.SimpleEventBus
import org.jordillonch.kes.cqrs.unit_of_work.domain.UoW
import org.jordillonch.kes.cqrs.unit_of_work.infrastructure.SimpleEffectQueue

/**
- A
  - B
    - C
  - D
 */
class UoWABDCTest : ShouldSpec(
    {
        should("execute test command handler then test1 event handler and finally test2 event handler") {
            val internalCommandBus = SimpleCommandBus()
            val internalEventBus = SimpleEventBus()
            val effectQueue = SimpleEffectQueue()
            val uow = UoW(internalCommandBus, internalEventBus, effectQueue)

            val commandBus = uow.commandBus
            val eventBus = uow.eventBus
            val executionOrder: MutableList<String> = ArrayList()

            val commandAHandler = TestACommandHandler(eventBus, executionOrder)
            val eventBHandler = TestBEventHandler(eventBus, executionOrder)
            val eventCHandler = TestCEventHandler(executionOrder)
            val eventDHandler = TestDEventHandler(executionOrder)
            commandBus.registerHandler(commandAHandler)
            eventBus.registerHandler(eventBHandler)
            eventBus.registerHandler(eventCHandler)
            eventBus.registerHandler(eventDHandler)

            commandBus.handle(TestACommand())

            executionOrder shouldBe listOf("A1", "A2", "A3", "B1", "B2", "D", "C")
        }
    }) {
    private class TestACommand : Command

    private class TestACommandHandler(
        private val eventBus: EventBus,
        private val executionOrder: MutableList<String>
    ) : CommandHandler<TestACommand> {

        override fun on(command: TestACommand) {
            executionOrder.add("A1")
            eventBus.publish(TestBEvent())
            executionOrder.add("A2")
            eventBus.publish(TestDEvent())
            executionOrder.add("A3")
        }
    }

    private class TestBEvent : Event
    private class TestCEvent : Event
    private class TestDEvent : Event

    private class TestBEventHandler(
        private val eventBus: EventBus,
        private val executionOrder: MutableList<String>
    ) : EventHandler<TestBEvent> {

        override fun on(event: TestBEvent) {
            executionOrder.add("B1")
            eventBus.publish(TestCEvent())
            executionOrder.add("B2")
        }
    }

    private class TestCEventHandler(private val executionOrder: MutableList<String>) : EventHandler<TestCEvent> {

        override fun on(event: TestCEvent) {
            executionOrder.add("C")
        }
    }

    private class TestDEventHandler(private val executionOrder: MutableList<String>) : EventHandler<TestDEvent> {

        override fun on(event: TestDEvent) {
            executionOrder.add("D")
        }
    }
}
