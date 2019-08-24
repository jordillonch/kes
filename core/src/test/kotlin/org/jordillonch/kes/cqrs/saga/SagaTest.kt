package org.jordillonch.kes.cqrs.saga

import io.kotlintest.specs.ShouldSpec
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.command.infrastructure.SimpleCommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventBus
import org.jordillonch.kes.cqrs.event.infrastructure.SimpleEventBus
import org.jordillonch.kes.cqrs.saga.domain.Saga
import org.jordillonch.kes.cqrs.saga.domain.SagaAssociationRepository
import org.jordillonch.kes.cqrs.saga.domain.SagaStateRepository
import org.jordillonch.kes.cqrs.saga.infrastructure.InMemorySagaAssociationRepository
import org.jordillonch.kes.cqrs.saga.infrastructure.InMemorySagaStateRepository
import org.jordillonch.kes.faker.Faker

class SagaTest : ShouldSpec(
    {
        should("register command and event handlers") {
            val commandBus = SimpleCommandBus()
            val eventBus = SimpleEventBus()
            val sagaAssociationRepository = InMemorySagaAssociationRepository()
            val sagaStateRepository = InMemorySagaStateRepository()
            val saga = TestSaga(commandBus, eventBus, sagaAssociationRepository, sagaStateRepository)

            val testCommandValue = Faker.instance().number().randomNumber()
            val command = TestCommand(testCommandValue)
            val testEventValue = Faker.instance().number().randomNumber()
            val event = TestEvent(testEventValue)

            commandBus.handle(command)
            eventBus.publish(event)

            assertThat(testCommandValue, equalTo(saga.testCommandValueToAssert))
            assertThat(testEventValue, equalTo(saga.testEventValueToAssert))
        }
    }
) {
    class TestSaga(
        commandBus: CommandBus,
        eventBus: EventBus,
        sagaAssociationRepository: SagaAssociationRepository,
        sagaStateRepository: SagaStateRepository
    ) : Saga
        (commandBus, eventBus, sagaAssociationRepository, sagaStateRepository) {

        override fun name() = "test_saga"

        var testCommandValueToAssert: Long? = null
        var testEventValueToAssert: Long? = null

        fun on(command: TestCommand) {
            testCommandValueToAssert = command.id
        }

        fun on(event: TestEvent) {
            testEventValueToAssert = event.id
        }
    }

    data class TestCommand(val id: Long) : Command
    data class TestEvent(val id: Long) : Event
}
