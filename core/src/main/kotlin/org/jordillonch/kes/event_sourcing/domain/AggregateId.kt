package org.jordillonch.kes.event_sourcing.domain

interface AggregateId {
    fun id(): String
}