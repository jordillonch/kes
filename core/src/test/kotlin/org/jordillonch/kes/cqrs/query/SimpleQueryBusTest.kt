package org.jordillonch.kes.cqrs.query

import io.kotlintest.shouldThrow
import io.kotlintest.specs.ShouldSpec
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
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

            assertThat(bus.ask(query).result, equalTo(testValue))
        }

        should("register a lambda handler and then query it") {
            val bus = SimpleQueryBus()

            bus.registerHandler { query: TestQuery -> TestQueryResult(query.id) }

            val testValue = Faker.instance().number().randomNumber()
            val query = TestQuery(testValue)

            assertThat(bus.ask(query).result, equalTo(testValue))
        }

        should("fail because no registered handler") {
            val bus = SimpleQueryBus()
            shouldThrow<NoQueryHandlerFoundException> {
                bus.ask(TestQuery(1))
            }
        }
    })

private data class TestQueryResult(val result: Long)

private data class TestQuery(val id: Long) : Query<TestQueryResult>

private class TestQueryHandler : QueryHandler<TestQueryResult, TestQuery> {
    override fun on(query: TestQuery): TestQueryResult {
        return TestQueryResult(query.id)
    }
}
