package org.jordillonch.kes.cqrs.bus.domain.association

interface AssociationIdsRepository {
    fun save(associationId: Any, entityId: Any)
    fun find(associationId: Any): List<Any>
}
