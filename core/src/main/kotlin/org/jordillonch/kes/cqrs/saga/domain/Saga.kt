package org.jordillonch.kes.cqrs.saga.domain

interface Saga {
    val id: SagaId
    fun name(): String
    fun newInstance(): Saga
}
