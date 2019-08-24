package org.jordillonch.kes.cqrs.saga.domain

interface SagaStateRepository {
    fun find(sagaId: SagaId): Map<String, Any?>?
    fun save(sagaId: SagaId, state: Map<String, Any?>): Any
}
