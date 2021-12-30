package org.jordillonch.kes.cqrs.bus

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.bus.domain.Command
import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.Event
import org.jordillonch.kes.cqrs.bus.domain.association.Associate
import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.domain.entity.*
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationIdsRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationTypesRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.BusSequential
import org.jordillonch.kes.cqrs.bus.infrastructure.GenericRepositoryInMemory
import java.util.*
import kotlin.reflect.KClass

class BusSequentialHandlersWithEntityTest : ShouldSpec({
    should("handle associated commands and events and recover entity state") {
        val associationIdsRepository = AssociationIdsRepositoryInMemory()
        val associationTypeRepository = AssociationTypesRepositoryInMemory()
        val associator = Associator(associationIdsRepository, associationTypeRepository)
        val genericRepository = GenericRepositoryInMemory()
        val bus = BusSequential(associator, genericRepository)

        val someStateRepository = SomeStateRepositoryInMemory()
        val testAssertionHandler = TestAssertionHandler()
        bus.register({ AHandlerWithEntity() }, someStateRepository)
        bus.register(testAssertionHandler)

        val startCommand = StartCommand()
        val secondCommand = SecondCommand(startCommand.otherId)
        val finishCommand = FinishCommand(startCommand.id)

        bus.push(startCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent FirstStepProcessed::class
        someStateRepository.find(startCommand.id) shouldBe FirstStepSomeEntity(startCommand.id)

        bus.push(secondCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent MiddleStep1Processed::class
        someStateRepository.find(startCommand.id) shouldBe SecondStepSomeEntity(startCommand.id)

        bus.push(secondCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent MiddleStep2Processed::class
        someStateRepository.find(startCommand.id) shouldBe FinalStepSomeEntity(startCommand.id)

        bus.push(finishCommand)
        bus.drain()
        testAssertionHandler shouldContainEvent FinalStepProcessed::class
        someStateRepository.find(startCommand.id) shouldBe null
    }
})

sealed class SomeEntity(val id_: UUID)
data class FirstStepSomeEntity(val id: UUID) : SomeEntity(id)
data class SecondStepSomeEntity(val id: UUID) : SomeEntity(id)
data class FinalStepSomeEntity(val id: UUID) : SomeEntity(id)

class AHandlerWithEntity {
    fun on(command: StartCommand): List<Effect> {
        val entity = FirstStepSomeEntity(command.id)
        return listOf(
            EntityCreated(entity),
            Associate(this, entity.id, SecondCommand::otherId, command.otherId),
            Associate(this, entity.id, FinishCommand::id, command.id),
            FirstStepProcessed()
        )
    }

    fun FirstStepSomeEntity.on(command: SecondCommand): List<Effect> {
        return listOf(
            EntityUpdated(evolveTo(::SecondStepSomeEntity)),
            MiddleStep1Processed()
        )
    }

    fun SecondStepSomeEntity.on(command: SecondCommand): List<Effect> {
        return listOf(
            EntityUpdated(evolveTo(::FinalStepSomeEntity)),
            MiddleStep2Processed()
        )
    }

    fun FinalStepSomeEntity.on(event: FinishCommand): List<Effect> {
        return listOf(
            EntityDeleted(this),
            FinalStepProcessed()
        )
    }
}

class SomeStateRepositoryInMemory : EntityTypedRepository<SomeEntity, UUID> {
    private val entityMap = mutableMapOf<UUID, SomeEntity>()

    override fun save(entity: SomeEntity) {
        entityMap[entity.id_] = entity
    }

    override fun find(id: UUID): SomeEntity? {
        return entityMap[id]
    }

    override fun delete(entity: SomeEntity) {
        entityMap.remove(entity.id_)
    }

    override fun entityType(): KClass<*> = SomeEntity::class
}


data class StartCommand(val id: UUID = UUID.randomUUID(), val otherId: UUID = UUID.randomUUID()) : Command
data class SecondCommand(val otherId: UUID = UUID.randomUUID()) : Command
data class FinishCommand(val id: UUID = UUID.randomUUID()) : Command

class FirstStepProcessed : Event
class MiddleStep1Processed : Event
class MiddleStep2Processed : Event
class FinalStepProcessed : Event
