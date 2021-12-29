package org.jordillonch.kes.cqrs.bus.infrastructure

import org.jordillonch.kes.cqrs.bus.domain.association.AssociationIdsRepository

class AssociationIdsRepositoryInMemory : AssociationIdsRepository {
    private val associations = mutableMapOf<Any, MutableList<Any>>()

    override fun save(associationId: Any, entityId: Any) {
        associations.computeIfAbsent(associationId) { mutableListOf() }.add(entityId)
    }

    override fun find(associationId: Any): List<Any> {
        return associations[associationId]?.toList() ?: emptyList()
    }
}
