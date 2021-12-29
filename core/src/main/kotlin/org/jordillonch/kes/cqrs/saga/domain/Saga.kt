package org.jordillonch.kes.cqrs.saga.domain

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.event.domain.Event
import kotlin.reflect.KProperty1

interface Saga {
    val id: SagaId
    fun name(): String
    fun newInstance(): Saga
}

data class SagaOutput(val nextState: SagaState, val effects: List<Effect>)
data class SagaLastOutput(val effects: List<Effect>)

data class Associate(
    val handler: Any,
    val entityId: Any,
    val associatedField: KProperty1<*, *>,
    val associatedId: Any,
//    val associationType: AssociationType
): Effect

class EndSaga: Event

enum class AssociationType {
    FROM_EFFECT,
    SINGLETON,
    ALL_INSTANCES
}
