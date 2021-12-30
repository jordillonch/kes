package org.jordillonch.kes.cqrs.bus.domain.entity

import kotlin.reflect.KClass

interface Repository<E, I> {
    fun save(entity: E)
    fun find(id: I): E?
    fun delete(entity: E)
}

interface EntityTypedRepository<E, I> : Repository<E, I> {
    fun entityType(): KClass<*>
}

interface GenericRepository : Repository<IdentifiedEntity, Any>

interface IdentifiedEntity {
    fun primaryId(): Any
}
