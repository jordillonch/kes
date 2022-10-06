package org.jordillonch.kes.cqrs.query

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jordillonch.kes.cqrs.query.domain.NoQueryHandlerFoundException
import org.jordillonch.kes.cqrs.query.domain.Query
import org.jordillonch.kes.cqrs.query.domain.QueryHandler
import org.jordillonch.kes.cqrs.query.infrastructure.SimpleQueryBus
import org.jordillonch.kes.faker.Faker

class SimpleQueryBusTest : ShouldSpec(
    {
        should("register a handler and then query it") {
            val bus = SimpleQueryBus()

            bus.registerHandler(TestQueryHandler())

            val testValue = Faker.instance().number().randomNumber()
            val query = TestQuery(testValue)

            bus.ask(query) shouldBe testValue
        }

        should("register a lambda handler and then query it") {
            val bus = SimpleQueryBus()

            bus.registerHandler { query: TestQuery -> query.id }

            val testValue = Faker.instance().number().randomNumber()
            val query = TestQuery(testValue)

            bus.ask(query) shouldBe testValue
        }

        should("fail because no registered handler") {
            val bus = SimpleQueryBus()
            shouldThrow<NoQueryHandlerFoundException> {
                bus.ask(TestQuery(1))
            }
        }
    })

private data class TestQuery(val id: Long) : Query<Long>

private class TestQueryHandler : QueryHandler<TestQuery, Long> {
    override fun on(query: TestQuery): Long {
        return query.id
    }
}
