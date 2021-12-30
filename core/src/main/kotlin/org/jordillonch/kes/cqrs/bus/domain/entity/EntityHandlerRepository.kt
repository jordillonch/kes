package org.jordillonch.kes.cqrs.bus.domain.entity

import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.Event
import kotlin.reflect.KClass

class EntityHandlerRepository(private val genericRepository: GenericRepository) {
    private val repositories: MutableMap<KClass<*>, Repository<Any, Any>> = mutableMapOf()

    fun <E, I> register(repository: Repository<E, I>, entityType: KClass<*>) {
        @Suppress("UNCHECKED_CAST")
        val castedRepository = repository as Repository<Any, Any>
        repositories[entityType] = castedRepository
        entityType.sealedSubclasses.forEach { repositories[it] = castedRepository }
    }

    fun on(event: EntityCreated): List<Effect> {
        save(event.entity)
        return emptyList()
    }

    fun on(event: EntityUpdated): List<Effect> {
        save(event.entity)
        return emptyList()
    }

    fun on(event: EntityDeleted): List<Effect> {
        repository(event.entity).delete(event.entity)
        return emptyList()
    }

    private fun save(entity: Any) {
        repository(entity).save(entity)
    }

    @Suppress("UNCHECKED_CAST")
    private fun repository(entity: Any): Repository<Any, Any> =
        repositories[entity.javaClass.kotlin] ?: genericRepository as Repository<Any, Any>
}

data class EntityCreated(val entity: Any): Event
data class EntityUpdated(val entity: Any): Event
data class EntityDeleted(val entity: Any): Event
