package org.jordillonch.kes.cqrs.saga

import io.kotlintest.specs.ShouldSpec
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.command.infrastructure.SimpleCommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.cqrs.event.infrastructure.SimpleEventBus
import org.jordillonch.kes.cqrs.saga.domain.Saga
import org.jordillonch.kes.cqrs.saga.domain.SagaStateRepository
import org.jordillonch.kes.cqrs.saga.infrastructure.InMemorySagaStateRepository
import java.util.UUID
import kotlin.test.assertTrue

class SagaAssociationsTest : ShouldSpec(
    {
        should("register command and event handlers") {
            val commandBus = SimpleCommandBus()
            val eventBus = SimpleEventBus()
            val sagaStateRepository = InMemorySagaStateRepository()
            val saga = TestAssociationSaga(commandBus, eventBus, sagaStateRepository)

            val testCommandId = UUID.randomUUID()
            val testCommand2Id = UUID.randomUUID()
            val testEventId = UUID.randomUUID()
            val command1 = TestCommand(testCommandId, testCommand2Id, testEventId)
            val command2 = TestCommand2(testCommand2Id)
            val event = TestEvent(testEventId)

            commandBus.handle(command1)
            commandBus.handle(command2)
            eventBus.publish(event)

            assertTrue(saga.testCommandIdCalled)
            assertTrue(saga.testCommand2IdCalled)
            assertTrue(saga.testEventCalled)
        }
    }
) {
    class TestAssociationSaga(commandBus: CommandBus, eventBus: EventBus, sagaStateRepository: SagaStateRepository) :
        Saga(commandBus, eventBus, sagaStateRepository) {

        override fun name() = "test_association_saga"

        var testCommandIdCalled = false
        var testCommand2IdCalled = false
        var testEventCalled = false

        fun on(command: TestCommand) {
            associate(TestCommand2::class, TestCommand2::testCommand2Id, command.testCommand2Id)
            associate(TestEvent::class, TestEvent::associatedId, command.eventId)
            testCommandIdCalled = true
        }

        fun on(command: TestCommand2) {
            testCommand2IdCalled = true
        }

        fun on(event: TestEvent) {
            testEventCalled = true
        }
    }

    data class TestCommand(val id: UUID, val testCommand2Id: UUID, val eventId: UUID) : Command
    data class TestCommand2(val testCommand2Id: UUID) : Command
    data class TestEvent(val associatedId: UUID) : Event
}
