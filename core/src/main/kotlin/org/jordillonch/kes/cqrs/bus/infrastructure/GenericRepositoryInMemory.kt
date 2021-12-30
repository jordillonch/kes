package org.jordillonch.kes.cqrs.bus.infrastructure

import org.jordillonch.kes.cqrs.bus.domain.entity.GenericRepository
import org.jordillonch.kes.cqrs.bus.domain.entity.IdentifiedEntity

class GenericRepositoryInMemory : GenericRepository {
    private val entityMap = mutableMapOf<Any, IdentifiedEntity>()

    override fun save(entity: IdentifiedEntity) {
        entityMap[entity.primaryId()] = entity
    }

    override fun find(id: Any): IdentifiedEntity? {
        return entityMap[id]
    }

    override fun delete(entity: IdentifiedEntity) {
        entityMap.remove(entity.primaryId())
    }
}
