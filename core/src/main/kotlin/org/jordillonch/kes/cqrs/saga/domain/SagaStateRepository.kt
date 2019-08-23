package org.jordillonch.kes.cqrs.saga.domain

import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.event.domain.Event
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface SagaStateRepository {
    fun associate(
        id: UUID,
        sagaName: String,
        effectKClass: KClass<*>,
        associatedProperty: KProperty1<*, UUID>,
        associatedPropertyValue: UUID
    )
    fun find(sagaName: String, command: Command): Map<String, Any>?
    fun find(sagaName: String, command: Event): Map<String, Any>?
    fun save(sagaId: UUID, state: Map<String, Any>): Any
}
