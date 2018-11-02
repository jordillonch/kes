package org.jordillonch.kes.cqrs.command

import io.kotlintest.shouldThrow
import io.kotlintest.specs.ShouldSpec
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandHandler
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import org.jordillonch.kes.cqrs.command.infrastructure.SimpleCommandBus
import org.jordillonch.kes.faker.Faker

class SimpleCommandBusTest : ShouldSpec(
        {
            should("register a handler and then query it") {
                val bus = SimpleCommandBus()

                val handler = TestCommandHandler()
                bus.registerHandler(handler)

                val testValue = Faker.instance().number().randomNumber()
                val command = TestCommand(testValue)

                bus.handle(command)
                assertThat(testValue, equalTo(handler.testValueToAssert))
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

    override fun on(command: TestCommand) {
        testValueToAssert = command.id
    }
}