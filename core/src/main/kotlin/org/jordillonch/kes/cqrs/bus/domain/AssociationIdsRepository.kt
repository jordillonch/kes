package org.jordillonch.kes.cqrs.bus.domain

interface AssociationIdsRepository {
    fun save(associationId: Any, entityId: Any)
    fun find(associationId: Any): List<Any>
}
