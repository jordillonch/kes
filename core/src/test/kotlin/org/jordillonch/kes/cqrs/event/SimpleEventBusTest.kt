package org.jordillonch.kes.cqrs.event

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventsHandler
import org.jordillonch.kes.cqrs.event.infrastructure.SimpleEventBus
import org.jordillonch.kes.faker.Faker

class SimpleEventBusTest : ShouldSpec(
    {
        should("register a handler and then send an event to it") {
            val bus = SimpleEventBus()

            val handler1 = TestEventsHandler()
            val handler2 = TestEventsHandler()
            bus.registerHandler(handler1)
            bus.registerHandler(handler2)

            val testValue = Faker.instance().number().randomNumber()
            val event = TestEvent(testValue)

            bus.publish(event)
            handler1.testValueToAssert shouldBe testValue
            handler2.testValueToAssert shouldBe testValue
        }

        should("register a lambda handler and then send an event to it") {
            val bus = SimpleEventBus()

            var handleTestValue1: Long? = null
            var handleTestValue2: Long? = null
            bus.registerHandler { event: TestEvent -> handleTestValue1 = event.id }
            bus.registerHandler { event: TestEvent -> handleTestValue2 = event.id }

            val testValue = Faker.instance().number().randomNumber()
            val event = TestEvent(testValue)

            bus.publish(event)
            handleTestValue1 shouldBe testValue
            handleTestValue2 shouldBe testValue
        }

        should("not fail because no registered handler") {
            val bus = SimpleEventBus()
            bus.publish(TestEvent(1))
        }
    })

private data class TestEvent(val id: Long) : Event

private class TestEventsHandler : EventsHandler<TestEvent> {
    var testValueToAssert: Long? = null

    fun on(event: TestEvent): List<Event> {
        testValueToAssert = event.id
        return emptyList()
    }
}
