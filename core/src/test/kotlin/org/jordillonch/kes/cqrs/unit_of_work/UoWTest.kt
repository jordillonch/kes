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

class UoWTest : ShouldSpec(
    {
        should("execute test command handler then test1 event handler and finally test2 event handler") {
            val internalCommandBus = SimpleCommandBus()
            val internalEventBus = SimpleEventBus()
            val effectQueue = SimpleEffectQueue()
            val uow = UoW(internalCommandBus, internalEventBus, effectQueue)

            val commandBus = uow.commandBus
            val eventBus = uow.eventBus
            val executionOrder: MutableList<Int> = ArrayList()

            val commandHandler = TestCommandHandler(eventBus, executionOrder)
            val event1Handler = Test1EventHandler(eventBus, executionOrder)
            val event2Handler = Test2EventHandler(executionOrder)
            commandBus.registerHandler(commandHandler)
            eventBus.registerHandler(event1Handler)
            eventBus.registerHandler(event2Handler)

            commandBus.handle(TestCommand())

            executionOrder shouldBe (1..5).toList()
        }
    }) {
    private class TestCommand : Command

    private class TestCommandHandler(
        private val eventBus: EventBus,
        private val executionOrder: MutableList<Int>
    ) : CommandHandler<TestCommand> {

        override fun on(command: TestCommand) {
            executionOrder.add(1)
            eventBus.publish(Test1Event())
            executionOrder.add(2)
        }
    }

    private class Test1Event : Event
    private class Test2Event : Event

    private class Test1EventHandler(
        private val eventBus: EventBus,
        private val executionOrder: MutableList<Int>
    ) : EventHandler<Test1Event> {

        override fun on(event: Test1Event) {
            executionOrder.add(3)
            eventBus.publish(Test2Event())
            executionOrder.add(4)
        }
    }

    private class Test2EventHandler(private val executionOrder: MutableList<Int>) : EventHandler<Test2Event> {

        override fun on(event: Test2Event) {
            executionOrder.add(5)
        }
    }
}
