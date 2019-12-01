package org.jordillonch.kes.cqrs.query.infrastructure

import org.jordillonch.kes.cqrs.query.domain.NoQueryHandlerFoundException
import org.jordillonch.kes.cqrs.query.domain.Query
import org.jordillonch.kes.cqrs.query.domain.QueryBus
import org.jordillonch.kes.cqrs.query.domain.QueryHandler
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

class SimpleQueryBus : QueryBus {
    private val handlers: MutableMap<String, (Query<Any>) -> Any> = mutableMapOf()

    override fun <R, Q: Query<R>> registerHandler(handler: QueryHandler<R, Q>) {
        @Suppress("UNCHECKED_CAST")
        handlers[classFrom(handler)] = { query: Query<Any> -> handler.on(query as Q) as Any }
    }

    override fun <R, Q: Query<R>> registerHandler(handler: (Q) -> R) {
        @Suppress("UNCHECKED_CAST")
        handlers[classFrom(handler)] = handler as (Query<Any>) -> Any
    }

    override fun <R> ask(query: Query<R>): R {
        @Suppress("UNCHECKED_CAST")
        return handlers[query::class.qualifiedName]
                       ?.invoke(query as Query<Any>) as R
               ?: throw NoQueryHandlerFoundException()
    }

    private fun <R, Q: Query<R>> classFrom(handler: (Q) -> R) =
        handler.reflect()!!.parameters.first().type.toString()

    private fun <R, Q: Query<R>> classFrom(handler: QueryHandler<R, Q>) =
            handler.javaClass.kotlin
                    .declaredFunctions
                    .firstFunctionNamedOn()
                    .mapParameterTypes()
                    .first { it.isSubclassOf(Query::class) }
                    .qualifiedName!!

    private fun Collection<KFunction<*>>.firstFunctionNamedOn() = first { it.name == "on" }

    private fun KFunction<*>.mapParameterTypes() = parameters.map { it.type.jvmErasure }
}
