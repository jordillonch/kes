package org.jordillonch.kes.cqrs.query.domain

import org.jordillonch.kes.cqrs.command.domain.Command

interface Query

interface QueryHandler<in Q : Query, R> {
    fun on(query: Q): R
}

interface QueryBus {
    fun <Q : Query> registerHandler(handler: QueryHandler<Q, *>)
    fun <Q : Query, R> registerHandler(handler: (Q) -> R)
    fun <R> ask(query: Query): R
}

class NoQueryHandlerFoundException : RuntimeException()
