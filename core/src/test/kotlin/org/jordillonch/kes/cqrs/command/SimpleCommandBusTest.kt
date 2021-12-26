package org.jordillonch.kes.cqrs.command

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandHandler
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import org.jordillonch.kes.cqrs.command.infrastructure.SimpleCommandBus
import org.jordillonch.kes.faker.Faker

class SimpleCommandBusTest : ShouldSpec(
    {
        should("register a handler and then handle it") {
            val bus = SimpleCommandBus()

            val handler = TestCommandHandler()
            bus.registerHandler(handler)

            val testValue = Faker.instance().number().randomNumber()
            val command = TestCommand(testValue)

            bus.handle(command)
            handler.testValueToAssert shouldBe testValue
        }

        should("register a lambda handler and then handle it") {
            val bus = SimpleCommandBus()

            var handleTestValue: Long? = null
            bus.registerHandler { command: TestCommand -> handleTestValue = command.id }

            val testValue = Faker.instance().number().randomNumber()
            val command = TestCommand(testValue)

            bus.handle(command)
            handleTestValue shouldBe testValue
        }

        should("register a lambda command-handler and then handle it") {
            val bus = SimpleCommandBus()

            var handleTestValue: Long? = null
            bus.registerHandler(TestCommand::class.java) { command: TestCommand -> handleTestValue = command.id }

            val testValue = Faker.instance().number().randomNumber()
            val command = TestCommand(testValue)

            bus.handle(command)
            handleTestValue shouldBe testValue
        }

        should("fail because no registered handler") {
            val bus = SimpleCommandBus()
            shouldThrow<NoCommandHandlerFoundException> {
                bus.handle(TestCommand(1))
            }
        }
    })

private data class TestCommand(val id: Long) : Command

private class TestCommandHandler : CommandHandler<TestCommand> {
    var testValueToAssert: Long? = null

    fun on(command: TestCommand): List<Effect> {
        testValueToAssert = command.id
        return emptyList()
    }
}
