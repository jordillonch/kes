package org.jordillonch.kes.cqrs.query.infrastructure

import org.jordillonch.kes.cqrs.query.domain.NoQueryHandlerFoundException
import org.jordillonch.kes.cqrs.query.domain.Query
import org.jordillonch.kes.cqrs.query.domain.QueryBus
import org.jordillonch.kes.cqrs.query.domain.QueryHandler
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class SimpleQueryBus : QueryBus {
    private val handlers: MutableMap<String, QueryHandler<Query, Any>> = mutableMapOf()

    override fun <Q : Query> registerHandler(handler: QueryHandler<Q, *>) {
        @Suppress("UNCHECKED_CAST")
        handlers[classFrom(handler)] = handler as QueryHandler<Query, Any>
    }

    override fun <R> ask(query: Query): R {
        @Suppress("UNCHECKED_CAST")
        return handlers[query::class.qualifiedName]
                       ?.on(query) as R
               ?: throw NoQueryHandlerFoundException()
    }

    private fun <Q : Query> classFrom(handler: QueryHandler<Q, *>) =
            handler.javaClass.kotlin
                    .declaredFunctions
                    .firstFunctionNamedOn()
                    .mapParameterTypes()
                    .first { it.isSubclassOf(Query::class) }
                    .qualifiedName!!

    private fun Collection<KFunction<*>>.firstFunctionNamedOn() = first { it.name == "on" }

    private fun KFunction<*>.mapParameterTypes() = parameters.map { it.type.jvmErasure }
}
