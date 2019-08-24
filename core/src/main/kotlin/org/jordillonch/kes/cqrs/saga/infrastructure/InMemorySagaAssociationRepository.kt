package org.jordillonch.kes.cqrs.saga.infrastructure

import org.jordillonch.kes.cqrs.Effect
import org.jordillonch.kes.cqrs.saga.domain.SagaAssociationRepository
import org.jordillonch.kes.cqrs.saga.domain.SagaId
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class InMemorySagaAssociationRepository : SagaAssociationRepository {
    private val storeEffectClassToPropertyName = mutableMapOf<SagaEffect, String>()
    private val storeEffectPropertyValueToSagaId = mutableMapOf<SagaEffectAssociationValue, SagaId>()

    override fun associate(
        sagaId: SagaId,
        sagaName: String,
        effectKClass: KClass<out Effect>,
        associatedProperty: KProperty1<*, Any>,
        associatedPropertyValue: Any
    ) {
        storeEffectClassToPropertyName[SagaEffect(sagaName, effectKClass)] = associatedProperty.name
        storeEffectPropertyValueToSagaId[
            SagaEffectAssociationValue(
                SagaEffect(sagaName, effectKClass),
                associatedPropertyValue
            )] = sagaId
    }

    override fun find(sagaName: String, effect: Effect): SagaId? {
        val sagaEffect = SagaEffect(sagaName, effect.javaClass.kotlin)
        return storeEffectClassToPropertyName[sagaEffect]
            ?.let { associatedPropertyName ->
                SagaEffectAssociationValue(
                    sagaEffect,
                    effect.javaClass
                        .getDeclaredField(associatedPropertyName)
                        .also { it.isAccessible = true }
                        .get(effect) as UUID
                )
                    .let { storeEffectPropertyValueToSagaId[it] }
            }
    }

    private data class SagaEffect(
        val sagaName: String,
        val effectKClass: KClass<out Effect>
    )

    private data class SagaEffectAssociationValue(
        val sagaEffect: SagaEffect,
        val associatedPropertyValue: Any
    )
}
