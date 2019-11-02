package org.jordillonch.kes.cqrs.saga.domain

import java.util.UUID

data class SagaId(val id: UUID) {
    companion object {
        fun new() = SagaId(UUID.randomUUID())
    }
}
