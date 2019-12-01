package org.jordillonch.kes.cqrs.query.domain

interface Query<R>

interface QueryHandler<R, Q: Query<R>> {
    fun on(query: Q): R
}

interface QueryBus {
    fun <R, Q: Query<R>> registerHandler(handler: QueryHandler<R, Q>)
    fun <R, Q: Query<R>> registerHandler(handler: (Q) -> R)
    fun <R> ask(query: Query<R>): R
}

class NoQueryHandlerFoundException : RuntimeException()
