package org.jordillonch.kes.cqrs.saga.domain

abstract class SagaState

interface SagaStateRepository {
    fun find(sagaId: SagaId): Map<String, SagaState?>?
    fun save(sagaId: SagaId, state: Map<String, SagaState?>)
}
