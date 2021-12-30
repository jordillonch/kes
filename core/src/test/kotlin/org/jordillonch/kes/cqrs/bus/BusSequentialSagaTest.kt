package org.jordillonch.kes.cqrs.bus

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.association.Associate
import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.domain.entity.IdentifiedEntity
import org.jordillonch.kes.cqrs.bus.domain.entity.evolveTo
import org.jordillonch.kes.cqrs.bus.domain.saga.SagaStateCreated
import org.jordillonch.kes.cqrs.bus.domain.saga.SagaStateDeleted
import org.jordillonch.kes.cqrs.bus.domain.saga.SagaStateUpdated
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationIdsRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationTypesRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.BusSequential
import org.jordillonch.kes.cqrs.bus.infrastructure.GenericRepositoryInMemory
import java.util.*

class BusSequentialSagaTest : ShouldSpec({
    should("handle associated commands and events and recover saga state") {
        val associationIdsRepository = AssociationIdsRepositoryInMemory()
        val associationTypeRepository = AssociationTypesRepositoryInMemory()
        val associator = Associator(associationIdsRepository, associationTypeRepository)
        val genericRepository = GenericRepositoryInMemory()
        val bus = BusSequential(associator, genericRepository)

        val testAssertionHandler = TestAssertionHandler()
        bus.register { TestSaga() }
        bus.register(testAssertionHandler)

        val startCommand = StartCommand()
        val secondCommand = SecondCommand(startCommand.otherId)
        val finishCommand = FinishCommand(startCommand.id)

        bus.push(startCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent FirstStepProcessed::class
        genericRepository.find(startCommand.id) shouldBe FirstStepSagaState(startCommand.id)

        bus.push(secondCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent MiddleStep1Processed::class
        genericRepository.find(startCommand.id) shouldBe SecondStepSagaState(startCommand.id)

        bus.push(secondCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent MiddleStep2Processed::class
        genericRepository.find(startCommand.id) shouldBe FinalStepSagaState(startCommand.id)

        bus.push(finishCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent FinalStepProcessed::class
        genericRepository.find(startCommand.id) shouldBe null
    }
})

sealed class TestSagaState(private val id_: UUID) : IdentifiedEntity {
    override fun primaryId(): Any = id_
}

data class FirstStepSagaState(val id: UUID) : TestSagaState(id)
data class SecondStepSagaState(val id: UUID) : TestSagaState(id)
data class FinalStepSagaState(val id: UUID) : TestSagaState(id)

class TestSaga {

    fun on(command: StartCommand): List<Effect> {
        val entity = FirstStepSagaState(command.id)
        return listOf(
            SagaStateCreated(entity),
            Associate(this, entity.id, SecondCommand::otherId, command.otherId),
            Associate(this, entity.id, FinishCommand::id, command.id),
            FirstStepProcessed()
        )
    }

    fun FirstStepSagaState.on(command: SecondCommand): List<Effect> {
        return listOf(
            SagaStateUpdated(evolveTo(::SecondStepSagaState)),
            MiddleStep1Processed()
        )
    }

    fun SecondStepSagaState.on(command: SecondCommand): List<Effect> {
        return listOf(
            SagaStateUpdated(evolveTo(::FinalStepSagaState)),
            MiddleStep2Processed()
        )
    }

    fun FinalStepSagaState.on(event: FinishCommand): List<Effect> {
        return listOf(
            SagaStateDeleted(this),
            FinalStepProcessed()
        )
    }
}
