package org.jordillonch.kes.cqrs.saga.domain

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.event.domain.Event
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface SagaAssociationRepository {
    fun associate(
        sagaId: SagaId,
        sagaName: String,
        effectKClass: KClass<out Effect>,
        associatedProperty: KProperty1<*, Any>,
        associatedPropertyValue: Any
    )
    fun find(sagaName: String, effect: Effect): SagaId?
}
