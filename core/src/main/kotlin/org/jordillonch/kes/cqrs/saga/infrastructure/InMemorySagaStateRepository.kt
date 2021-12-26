package org.jordillonch.kes.cqrs.saga.infrastructure

import org.jordillonch.kes.cqrs.saga.domain.SagaId
import org.jordillonch.kes.cqrs.saga.domain.SagaState
import org.jordillonch.kes.cqrs.saga.domain.SagaStateRepository

class InMemorySagaStateRepository : SagaStateRepository {
    private val storeSagaState = mutableMapOf<SagaId, Map<String, SagaState?>>()

    override fun save(sagaId: SagaId, state: Map<String, SagaState?>) {
        storeSagaState[sagaId] = state
    }

    override fun find(sagaId: SagaId): Map<String, SagaState?>? = storeSagaState[sagaId]
}
