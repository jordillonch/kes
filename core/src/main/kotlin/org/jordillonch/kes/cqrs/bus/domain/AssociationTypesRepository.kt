package org.jordillonch.kes.cqrs.bus.domain

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface AssociationTypesRepository {
    fun save(handler: KClass<*>, associatedKlass: KClass<*>, associatedField: KProperty1<*, *>)
    fun find(handler: KClass<*>, associatedKlass: KClass<*>): KProperty1<*, *>
}
