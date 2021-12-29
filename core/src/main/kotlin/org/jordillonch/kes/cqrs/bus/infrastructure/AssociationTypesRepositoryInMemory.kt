package org.jordillonch.kes.cqrs.bus.infrastructure

import org.jordillonch.kes.cqrs.bus.domain.AssociationTypesRepository
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class AssociationTypesRepositoryInMemory : AssociationTypesRepository {
    private val handlerAndAssociatedKlassToAssociatedField = mutableMapOf<Pair<*, KClass<*>>, KProperty1<*, *>>()

    override fun save(handler: KClass<*>, associatedKlass: KClass<*>, associatedField: KProperty1<*, *>) {
        handlerAndAssociatedKlassToAssociatedField[handler to associatedKlass] = associatedField
    }

    override fun find(handler: KClass<*>, associatedKlass: KClass<out Any>): KProperty1<*, *> {
        return handlerAndAssociatedKlassToAssociatedField[handler to associatedKlass]
            ?: throw IllegalArgumentException("Association $handler to $associatedKlass not found")
    }
}
