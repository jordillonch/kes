package org.jordillonch.kes.cqrs.saga

import io.kotlintest.specs.ShouldSpec
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.infrastructure.SimpleCommandBus
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.infrastructure.SimpleEventBus
import org.jordillonch.kes.cqrs.saga.domain.Saga
import org.jordillonch.kes.cqrs.saga.domain.SagaId
import org.jordillonch.kes.cqrs.saga.domain.SagaRegister
import org.jordillonch.kes.cqrs.saga.infrastructure.InMemorySagaAssociationRepository
import org.jordillonch.kes.cqrs.saga.infrastructure.InMemorySagaStateRepository
import java.util.UUID
import kotlin.test.assertNull

class SagaTest : ShouldSpec(
    {
        should("handle associated commands and events and recover saga state") {
            val commandBus = SimpleCommandBus()
            val eventBus = SimpleEventBus()
            val sagaAssociationRepository = InMemorySagaAssociationRepository()
            val sagaStateRepository = InMemorySagaStateRepository()
            val sagaRegister = SagaRegister(commandBus, eventBus, sagaAssociationRepository, sagaStateRepository)
            TestSaga(sagaRegister)

            TestSaga.testCommand1IdForAssert = null
            TestSaga.testCommand2IdForAssert = null
            TestSaga.testEventIdForAssert = null

            val testCommandId = UUID.randomUUID()
            val testCommand2Id = UUID.randomUUID()
            val testEventId = UUID.randomUUID()
            val command1 = TestCommand(testCommandId, testCommand2Id, testEventId)
            val command2 = TestCommand2(testCommand2Id)
            val event = TestEvent(testEventId)

            commandBus.handle(command1)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(testCommandId))
            assertNull(TestSaga.testCommand2IdForAssert)
            assertNull(TestSaga.testEventIdForAssert)

            commandBus.handle(command2)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(testCommandId))
            assertThat(TestSaga.testCommand2IdForAssert, equalTo(testCommand2Id))
            assertNull(TestSaga.testEventIdForAssert)

            eventBus.publish(event)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(testCommandId))
            assertThat(TestSaga.testCommand2IdForAssert, equalTo(testCommand2Id))
            assertThat(TestSaga.testEventIdForAssert, equalTo(testEventId))
        }

        should("handle two saga instances") {
            val commandBus = SimpleCommandBus()
            val eventBus = SimpleEventBus()
            val sagaAssociationRepository = InMemorySagaAssociationRepository()
            val sagaStateRepository = InMemorySagaStateRepository()
            val sagaRegister = SagaRegister(commandBus, eventBus, sagaAssociationRepository, sagaStateRepository)
            TestSaga(sagaRegister)

            TestSaga.testCommand1IdForAssert = null
            TestSaga.testCommand2IdForAssert = null
            TestSaga.testEventIdForAssert = null

            val saga1TestCommandId = UUID.randomUUID()
            val saga1TestCommand2Id = UUID.randomUUID()
            val saga1TestEventId = UUID.randomUUID()
            val saga1Command1 = TestCommand(saga1TestCommandId, saga1TestCommand2Id, saga1TestEventId)
            val saga1Command2 = TestCommand2(saga1TestCommand2Id)
            val saga1Event = TestEvent(saga1TestEventId)

            val saga2TestCommandId = UUID.randomUUID()
            val saga2TestCommand2Id = UUID.randomUUID()
            val saga2TestEventId = UUID.randomUUID()
            val saga2Command1 = TestCommand(saga2TestCommandId, saga2TestCommand2Id, saga2TestEventId)
            val saga2Command2 = TestCommand2(saga2TestCommand2Id)
            val saga2Event = TestEvent(saga2TestEventId)

            commandBus.handle(saga1Command1)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(saga1TestCommandId))
            assertNull(TestSaga.testCommand2IdForAssert)
            assertNull(TestSaga.testEventIdForAssert)

            commandBus.handle(saga2Command1)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(saga2TestCommandId))
            assertNull(TestSaga.testCommand2IdForAssert)
            assertNull(TestSaga.testEventIdForAssert)


            commandBus.handle(saga1Command2)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(saga1TestCommandId))
            assertThat(TestSaga.testCommand2IdForAssert, equalTo(saga1TestCommand2Id))
            assertNull(TestSaga.testEventIdForAssert)

            commandBus.handle(saga2Command2)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(saga2TestCommandId))
            assertThat(TestSaga.testCommand2IdForAssert, equalTo(saga2TestCommand2Id))
            assertNull(TestSaga.testEventIdForAssert)


            eventBus.publish(saga1Event)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(saga1TestCommandId))
            assertThat(TestSaga.testCommand2IdForAssert, equalTo(saga1TestCommand2Id))
            assertThat(TestSaga.testEventIdForAssert, equalTo(saga1TestEventId))

            eventBus.publish(saga2Event)
            assertThat(TestSaga.testCommand1IdForAssert, equalTo(saga2TestCommandId))
            assertThat(TestSaga.testCommand2IdForAssert, equalTo(saga2TestCommand2Id))
            assertThat(TestSaga.testEventIdForAssert, equalTo(saga2TestEventId))
        }
    }
) {
    class TestSaga(private val sagaRegister: SagaRegister) : Saga {
        override val id = SagaId.new()
        override fun name() = "test_saga"
        override fun newInstance() = TestSaga(sagaRegister)

        init {
            sagaRegister.init(this)
        }

        var testCommand1Id: UUID? = null
        var testCommand2Id: UUID? = null
        var testEventId: UUID? = null

        companion object {
            var testCommand1IdForAssert: UUID? = null
            var testCommand2IdForAssert: UUID? = null
            var testEventIdForAssert: UUID? = null
        }

        fun on(command: TestCommand) {
            sagaRegister.associate(this, TestCommand2::class, TestCommand2::testCommand2Id, command.testCommand2Id)
            sagaRegister.associate(this, TestEvent::class, TestEvent::associatedId, command.eventId)
            testCommand1Id = command.id
            copyPropertiesToCompanionObjects()
        }

        fun on(command: TestCommand2) {
            testCommand2Id = command.testCommand2Id
            copyPropertiesToCompanionObjects()
        }

        fun on(event: TestEvent) {
            testEventId = event.associatedId
            copyPropertiesToCompanionObjects()
        }

        private fun copyPropertiesToCompanionObjects() {
            testCommand1IdForAssert = testCommand1Id
            testCommand2IdForAssert = testCommand2Id
            testEventIdForAssert = testEventId
        }
    }

    data class TestCommand(val id: UUID, val testCommand2Id: UUID, val eventId: UUID) : Command
    data class TestCommand2(val testCommand2Id: UUID) : Command
    data class TestEvent(val associatedId: UUID) : Event
}
