package org.jordillonch.kes.cqrs.saga.domain

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.event.domain.Event
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface Saga {
    val id: SagaId
    fun name(): String
    fun newInstance(): Saga
}

data class SagaOutput(val nextState: SagaState, val effects: List<Effect>)
data class SagaLastOutput(val effects: List<Effect>)

data class Associate(
    val effectKClass: KClass<out Effect>,
    val associatedProperty: KProperty1<*, Any>,
    val associatedPropertyValue: Any
): Effect

class EndSaga: Event
