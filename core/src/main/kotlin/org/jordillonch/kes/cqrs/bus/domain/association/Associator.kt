package org.jordillonch.kes.cqrs.bus.domain.association

import org.jordillonch.kes.cqrs.bus.domain.Effect
import org.jordillonch.kes.cqrs.bus.domain.EffectsHandler
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandler
import org.jordillonch.kes.cqrs.saga.domain.Associate
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

class Associator(
    private val associationRepository: AssociationIdsRepository,
    private val associationTypesRepository: AssociationTypesRepository
) {
    fun associate(association: Associate) {
        associationRepository.save(association.associatedId, association.entityId)
        val associatedKClass = association.associatedField.javaField!!.declaringClass.kotlin
        associationTypesRepository.save(
            association.handler.javaClass.kotlin,
            associatedKClass,
            association.associatedField
        )
    }

    fun entityIdsFor(handler: KClass<out EntityHandler>, associatedType: Any): List<Any> {
        val associatedFieldName = associationTypesRepository.find(handler, associatedType.javaClass.kotlin).name
        val entityIdToFind = associatedType.javaClass.kotlin.declaredMemberProperties
            .find { it.name == associatedFieldName }!!
            .get(associatedType)!!
        return associationRepository.find(entityIdToFind)
    }

    fun handler(): EffectsHandler {
        return object : EffectsHandler {
            fun on(effect: Associate): List<Effect> {
                associate(effect)
                return emptyList()
            }
        }
    }
}
