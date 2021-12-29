package org.jordillonch.kes.cqrs.bus.domain

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.command.domain.EffectHandler
import org.jordillonch.kes.cqrs.command.domain.EntityCreated
import org.jordillonch.kes.cqrs.command.domain.EntityDeleted
import org.jordillonch.kes.cqrs.command.domain.EntityUpdated
import kotlin.reflect.KClass

class EntityHandlerRepository : EffectHandler {
    val repositories: MutableMap<KClass<*>, Repository<Any, Any>> = mutableMapOf()

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

    private fun repository(entity: Any): Repository<Any, Any> =
        repositories[entity.javaClass.kotlin]
            ?: throw IllegalStateException("No repository registered for ${entity.javaClass.kotlin}")
}
