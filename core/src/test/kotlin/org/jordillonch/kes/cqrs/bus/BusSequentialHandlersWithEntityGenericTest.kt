package org.jordillonch.kes.cqrs.bus

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.association.Associate
import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityCreated
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityDeleted
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityUpdated
import org.jordillonch.kes.cqrs.bus.domain.entity.IdentifiedEntity
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationIdsRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationTypesRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.BusSequential
import org.jordillonch.kes.cqrs.bus.infrastructure.GenericRepositoryInMemory
import java.util.*

class BusSequentialHandlersWithEntityGenericTest : ShouldSpec({
    should("handle associated commands and events and recover entity state") {
        val associationIdsRepository = AssociationIdsRepositoryInMemory()
        val associationTypeRepository = AssociationTypesRepositoryInMemory()
        val associator = Associator(associationIdsRepository, associationTypeRepository)
        val genericRepository = GenericRepositoryInMemory()
        val bus = BusSequential(associator, genericRepository)

        val testAssertionHandler = TestAssertionHandler()
        bus.register { AHandlerWithGenericEntity() }
        bus.register(testAssertionHandler)

        val startCommand = StartCommand()
        val secondCommand = SecondCommand(startCommand.otherId)
        val finishCommand = FinishCommand(startCommand.id)

        bus.push(startCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent FirstStepProcessed::class
        genericRepository.find(startCommand.id) shouldBe FirstStepAnotherEntity(startCommand.id)

        bus.push(secondCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent MiddleStep1Processed::class
        genericRepository.find(startCommand.id) shouldBe SecondStepAnotherEntity(startCommand.id)

        bus.push(secondCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent MiddleStep2Processed::class
        genericRepository.find(startCommand.id) shouldBe FinalStepAnotherEntity(startCommand.id)

        bus.push(finishCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent FinalStepProcessed::class
        genericRepository.find(startCommand.id) shouldBe null
    }
})

sealed class AnotherEntity(private val id_: UUID): IdentifiedEntity {
    override fun primaryId(): Any = id_
}

data class FirstStepAnotherEntity(val id: UUID) : AnotherEntity(id)
data class SecondStepAnotherEntity(val id: UUID) : AnotherEntity(id)
data class FinalStepAnotherEntity(val id: UUID) : AnotherEntity(id)

class AHandlerWithGenericEntity {

    fun on(command: StartCommand): List<Effect> {
        val entity = FirstStepAnotherEntity(command.id)
        return listOf(
            EntityCreated(entity),
            Associate(this, entity.id, SecondCommand::otherId, command.otherId),
            Associate(this, entity.id, FinishCommand::id, command.id),
            FirstStepProcessed()
        )
    }

    fun FirstStepAnotherEntity.on(command: SecondCommand): List<Effect> {
        return listOf(
            // TODO: use some kind of "evolve"
            EntityUpdated(SecondStepAnotherEntity(id)),
            MiddleStep1Processed()
        )
    }

    fun SecondStepAnotherEntity.on(command: SecondCommand): List<Effect> {
        return listOf(
            // TODO: use some kind of "evolve"
            EntityUpdated(FinalStepAnotherEntity(id)),
            MiddleStep2Processed()
        )
    }

    fun FinalStepAnotherEntity.on(event: FinishCommand): List<Effect> {
        return listOf(
            EntityDeleted(this),
            FinalStepProcessed()
        )
    }
}
