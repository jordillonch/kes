package org.jordillonch.kes.cqrs.event

import io.kotlintest.specs.ShouldSpec
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.event.domain.EventHandler
import org.jordillonch.kes.cqrs.event.infrastructure.SimpleEventBus
import org.jordillonch.kes.faker.Faker

class SimpleEventBusTest : ShouldSpec(
    {
        should("register a handler and then send an event to it") {
            val bus = SimpleEventBus()

            val handler1 = TestEventHandler()
            val handler2 = TestEventHandler()
            bus.registerHandler(handler1)
            bus.registerHandler(handler2)

            val testValue = Faker.instance().number().randomNumber()
            val event = TestEvent(testValue)

            bus.publish(event)
            assertThat(testValue, equalTo(handler1.testValueToAssert))
            assertThat(testValue, equalTo(handler2.testValueToAssert))
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
            assertThat(testValue, equalTo(handleTestValue1))
            assertThat(testValue, equalTo(handleTestValue2))
        }

        should("not fail because no registered handler") {
            val bus = SimpleEventBus()
            bus.publish(TestEvent(1))
        }
    })

private data class TestEvent(val id: Long) : Event

private class TestEventHandler : EventHandler<TestEvent> {
    var testValueToAssert: Long? = null

    override fun on(event: TestEvent) {
        testValueToAssert = event.id
    }
}
