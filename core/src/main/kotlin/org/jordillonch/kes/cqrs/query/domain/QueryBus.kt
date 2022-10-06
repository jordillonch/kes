package org.jordillonch.kes.cqrs.query.domain

interface Query<R>

interface QueryHandler<in Q : Query<R>, R> {
    fun on(query: Q): R
}

interface QueryBus {
    fun <Q : Query<*>> registerHandler(handler: QueryHandler<Q, *>)
    fun <Q : Query<R>, R> registerHandler(handler: (Q) -> R)
    fun <R> ask(query: Query<R>): R
}

class NoQueryHandlerFoundException : RuntimeException()
