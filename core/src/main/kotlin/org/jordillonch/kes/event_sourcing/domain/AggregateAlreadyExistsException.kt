package org.jordillonch.kes.event_sourcing.domain

import org.jordillonch.kes.ddd.domain.DomainException

class AggregateAlreadyExistsException(errorCode: String, errorMessage: String) : DomainException(errorCode, errorMessage) {
    companion object {
        fun appendingFirstEvent(aggregate: Aggregate) =
                AggregateAlreadyExistsException("aggregate already exists", "Aggregate already exists <$aggregate>")
    }
}