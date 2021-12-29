package org.jordillonch.kes.cqrs.bus

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.bus.domain.Associator
import org.jordillonch.kes.cqrs.bus.domain.EntityHandler
import org.jordillonch.kes.cqrs.bus.domain.Repository
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationIdsRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationTypesRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.BusSequential
import org.jordillonch.kes.cqrs.command.SimpleBusTest
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.EntityCreated
import org.jordillonch.kes.cqrs.command.domain.EntityDeleted
import org.jordillonch.kes.cqrs.command.domain.EntityUpdated
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventHandler
import org.jordillonch.kes.cqrs.saga.domain.Associate
import java.util.*
import kotlin.reflect.KClass

class HandlersWithStateSimpleBusTest : ShouldSpec({
    should("handle associated commands and events and recover saga state") {
        val associationIdsRepository = AssociationIdsRepositoryInMemory()
        val associationTypeRepository = AssociationTypesRepositoryInMemory()
        val associator = Associator(associationIdsRepository, associationTypeRepository)
        val bus = BusSequential(associator)

        val someStateRepository = SomeStateRepositoryInMemory()
        val testAssertionHandler = TestAssertionHandler()
        bus.register(AHandlerWithEntity::class, { AHandlerWithEntity() }, someStateRepository)
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

class AHandlerWithEntity : EntityHandler {

    override fun newInstance(): EntityHandler {
        // here we can choose to create a new one each time or reuse the same one
        return AHandlerWithEntity()
    }

    fun firstStep(command: StartCommand): List<Effect> {
        val entity = FirstStepSomeEntity(command.id)
        return listOf(
            EntityCreated(entity),
            Associate(this, entity.id, SecondCommand::otherId, command.otherId),
            Associate(this, entity.id, FinishCommand::id, command.id),
            FirstStepProcessed()
        )
    }

    fun middleStep(entity: FirstStepSomeEntity, command: SecondCommand): List<Effect> {
        return listOf(
            // TODO: use some kind of "evolve"
            EntityUpdated(SecondStepSomeEntity(entity.id)),
            MiddleStep1Processed()
        )
    }

    fun middleStep(entity: SecondStepSomeEntity, command: SecondCommand): List<Effect> {
        return listOf(
            // TODO: use some kind of "evolve"
            EntityUpdated(FinalStepSomeEntity(entity.id)),
            MiddleStep2Processed()
        )
    }

    fun finalStep(entity: FinalStepSomeEntity, event: FinishCommand): List<Effect> {
        return listOf(
            EntityDeleted(entity),
            FinalStepProcessed()
        )
    }
}

class SomeStateRepositoryInMemory : Repository<SomeEntity, UUID> {
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

class TestAssertionHandler : EventHandler<SimpleBusTest.ATestEvent> {
    private val events: MutableList<Event> = mutableListOf()

    fun on(event: Event): List<Effect> {
        events.add(event)
        return emptyList()
    }

    fun getEvents(): List<Event> = events

    infix fun shouldContainEvent(kClass: KClass<out Event>) {
        getEvents().map { it.javaClass.kotlin } shouldContain kClass
    }
}
