package org.jordillonch.kes.cqrs.command

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.bus.domain.Command
import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.association.Associator
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationIdsRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.AssociationTypesRepositoryInMemory
import org.jordillonch.kes.cqrs.bus.infrastructure.BusSequential
import org.jordillonch.kes.cqrs.command.domain.CommandHandler
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventsHandler
import org.jordillonch.kes.faker.Faker

class SimpleBusTest : ShouldSpec(
    {
        should("register some handlers and then handle them") {
            val associationIdsRepository = AssociationIdsRepositoryInMemory()
            val associationTypeRepository = AssociationTypesRepositoryInMemory()
            val associator = Associator(associationIdsRepository, associationTypeRepository)
            val bus = BusSequential(associator)

            val handler1 = ATestHandler()
            val handler2 = AnotherTestHandler()
            bus.register(handler1)
            bus.register(handler2)

            val command = ATestCommand(Faker.instance().number().randomNumber())
            val event = ATestEvent(Faker.instance().number().randomNumber())
            val otherCommand = AnotherTestCommand(Faker.instance().number().randomNumber())

            bus.push(command)
            bus.push(event)
            bus.drain()
            handler1.testValueToAssert shouldBe command.id
            handler1.testProcessed shouldBe false
            handler2.testValueToAssert shouldBe event.id

            bus.push(otherCommand)
            bus.drain()
            handler1.testValueToAssert shouldBe otherCommand.id
            handler1.testProcessed shouldBe true
        }

        should("fail because no registered command handler") {
            val associationIdsRepository = AssociationIdsRepositoryInMemory()
            val associationTypeRepository = AssociationTypesRepositoryInMemory()
            val associator = Associator(associationIdsRepository, associationTypeRepository)
            val bus = BusSequential(associator)
            bus.push(ATestEvent(1))
            bus.drain()
            shouldThrow<NoCommandHandlerFoundException> {
                bus.push(ATestCommand(1))
                bus.drain()
            }
        }
    }) {

    data class ATestCommand(val id: Long) : Command
    data class AnotherTestCommand(val id: Long) : Command
    data class ATestEvent(val id: Long) : Event
    class ProcessedTestEvent : Event

    class ATestHandler : CommandHandler<ATestCommand> {
        var testValueToAssert: Long? = null
        var testProcessed: Boolean = false

        fun on(command: ATestCommand): List<Effect> {
            testValueToAssert = command.id
            return emptyList()
        }

        fun on(command: AnotherTestCommand): List<Effect> {
            testValueToAssert = command.id
            return listOf(ProcessedTestEvent())
        }

        fun on(event: ProcessedTestEvent): List<Effect> {
            testProcessed = true
            return emptyList()
        }
    }


    class AnotherTestHandler : EventsHandler<ATestEvent> {
        var testValueToAssert: Long? = null

        fun on(event: ATestEvent): List<Effect> {
            testValueToAssert = event.id
            return emptyList()
        }
    }
}
