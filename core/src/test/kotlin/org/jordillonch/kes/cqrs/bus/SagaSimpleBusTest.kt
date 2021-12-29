//package org.jordillonch.kes.cqrs.bus
//
//import io.kotest.core.spec.style.ShouldSpec
//import org.jordillonch.kes.cqrs.command.domain.Command
//import org.jordillonch.kes.cqrs.bus.infrastructure.SimpleBus
//import org.jordillonch.kes.cqrs.event.domain.Event
//import org.jordillonch.kes.cqrs.saga.domain.Saga
//import org.jordillonch.kes.cqrs.saga.domain.SagaState
//import java.util.*
//
//class SagaSimpleBusTest : ShouldSpec({
//    should("handle associated commands and events and recover saga state") {
//        val bus = SimpleBus()
//        bus.register(TestSaga::class)
//
//        val testCommandId = UUID.randomUUID()
//        val testCommand2Id = UUID.randomUUID()
//        val testEventId = UUID.randomUUID()
//        val startSagaCommand = StartSagaCommand(testCommandId, testCommand2Id, testEventId)
//        val command2 = SecondCommand(testCommand2Id)
//        val event = TestEvent(testEventId)
//
//        bus.push(startSagaCommand)
//        bus.drain()
//        bus.findSagaState(TestSaga::class, startSagaCommand.id) shouldBe TestSagaState(testCommandId, null, null)
//
//        bus.push(command2)
//        bus.drain()
//        bus.findSagaState(TestSaga::class, startSagaCommand.id) shouldBe TestSagaState(
//            testCommandId,
//            testCommand2Id,
//            null
//        )
//
//        bus.push(event)
//        bus.drain()
//        bus.findSagaState(TestSaga::class, startSagaCommand.id) shouldBe TestSagaState(
//            testCommandId,
//            testCommand2Id,
//            testEventId
//        )
//    }
//
//    should("handle two saga instances") {
//
//        val saga1TestCommandId = UUID.randomUUID()
//        val saga1TestCommand2Id = UUID.randomUUID()
//        val saga1TestEventId = UUID.randomUUID()
//        val saga1Command1 = StartSagaCommand(saga1TestCommandId, saga1TestCommand2Id, saga1TestEventId)
//        val saga1Command2 = SecondCommand(saga1TestCommand2Id)
//        val saga1Event = TestEvent(saga1TestEventId)
//
//        val saga2TestCommandId = UUID.randomUUID()
//        val saga2TestCommand2Id = UUID.randomUUID()
//        val saga2TestEventId = UUID.randomUUID()
//        val saga2Command1 = StartSagaCommand(saga2TestCommandId, saga2TestCommand2Id, saga2TestEventId)
//        val saga2Command2 = SecondCommand(saga2TestCommand2Id)
//        val saga2Event = TestEvent(saga2TestEventId)
//
//        commandBus.handle(saga1Command1)
//        sagaStateRepository.find(testSaga.id) shouldBe TestSagaState(saga1TestCommandId, null, null)
//
//        commandBus.handle(saga2Command1)
//        sagaStateRepository.find(testSaga.id) shouldBe TestSagaState(saga2TestCommandId, null, null)
//
//        commandBus.handle(saga1Command2)
//        sagaStateRepository.find(testSaga.id) shouldBe TestSagaState(saga1TestCommandId, saga1TestCommand2Id, null)
//
//        commandBus.handle(saga2Command2)
//        sagaStateRepository.find(testSaga.id) shouldBe TestSagaState(saga2TestCommandId, saga2TestCommand2Id, null)
//
//        eventBus.publish(saga1Event)
//        sagaStateRepository.find(testSaga.id) shouldBe TestSagaState(
//            saga1TestCommandId,
//            saga1TestCommand2Id,
//            saga1TestEventId
//        )
//
//        eventBus.publish(saga2Event)
//        sagaStateRepository.find(testSaga.id) shouldBe TestSagaState(
//            saga2TestCommandId,
//            saga2TestCommand2Id,
//            saga2TestEventId
//        )
//    }
//}) {
//
//    data class TestSagaState(
//        val testCommand1Id: UUID?,
//        val testCommand2Id: UUID?,
//        val testEventId: UUID?,
//        val testEventReceivedByEverybodyId: UUID?
//    ) : SagaState()
//
//    class TestSaga(private val sagaRegister: SagaRegister) : Saga {
//        override val id = SagaId.new()
//        override fun name() = "test_saga"
//        override fun newInstance() = TestSaga(sagaRegister)
//
//        init {
//            sagaRegister.init(this)
//        }
//
//        fun on(command: StartSagaCommand): SagaOutput {
//            return SagaOutput(
//                TestSagaState(command.id, null, null),
//                listOf(
//                    Associate(SecondCommand::class, SecondCommand::testCommand2Id, command.testCommand2Id),
//                    Associate(TestEvent::class, TestEvent::associatedId, command.eventId),
//                    TestEffectProcessed(command.id)
//                )
//            )
//        }
//
//        fun on(state: TestSagaState, command: SecondCommand): SagaOutput {
//            return SagaOutput(
//                state.copy(testCommand2Id = command.testCommand2Id),
//                listOf(TestEffectProcessed(command.testCommand2Id))
//            )
//        }
//
//        fun on(state: TestSagaState, event: TestEvent): SagaOutput {
//            return SagaOutput(
//                state.copy(testEventId = event.associatedId),
//                listOf(TestEffectProcessed(event.associatedId))
//            )
//        }
//
//        fun forAny(state: TestSagaState, event: TestEventReceivedByEverybody): SagaOutput {
//            return SagaOutput(
//                state.copy(testEventReceivedByEverybodyId = event.id),
//                listOf(TestEffectProcessed(event.id))
//            )
//        }
//
//        fun on(state: TestSagaState, event: FinishCommand): SagaLastOutput {
//            return SagaLastOutput(listOf(TestEffectProcessed(event.id), EndSaga()))
//        }
//
//    }
//
//    data class StartSagaCommand(val id: UUID, val testCommand2Id: UUID, val eventId: UUID) : Command
//    data class SecondCommand(val testCommand2Id: UUID) : Command
//    data class TestEvent(val associatedId: UUID) : Event
//    data class TestEventReceivedByEverybody(val id: UUID) : Event
//    data class TestEffectProcessed(val id: UUID) : Event
//    data class FinishCommand(val id: UUID) : Command
//}
