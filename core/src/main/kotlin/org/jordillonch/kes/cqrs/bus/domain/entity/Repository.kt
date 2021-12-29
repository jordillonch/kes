package org.jordillonch.kes.cqrs.bus.domain.entity

import kotlin.reflect.KClass

interface Repository<E, I> {
    fun save(entity: E)
    fun find(id: I): E?
    fun delete(entity: E)
    fun entityType(): KClass<*>
}
