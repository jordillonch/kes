package org.jordillonch.kes.cqrs.saga.infrastructure

import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.event.domain.Event
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
        effectKClass: KClass<*>,
        associatedProperty: KProperty1<*, UUID>,
        associatedPropertyValue: UUID
    ) {
        storeEffectClassToPropertyName[SagaEffect(sagaName, effectKClass)] = associatedProperty.name
        storeEffectPropertyValueToSagaId[
            SagaEffectAssociationValue(
                SagaEffect(sagaName, effectKClass),
                associatedPropertyValue
            )] = sagaId
    }

    override fun find(sagaName: String, command: Command): SagaId? = findAny(sagaName, command)

    override fun find(sagaName: String, event: Event): SagaId? = findAny(sagaName, event)

    private fun findAny(sagaName: String, effect: Any): SagaId? {
        val sagaEffect = SagaEffect(sagaName, effect.javaClass.kotlin)
        return storeEffectClassToPropertyName[sagaEffect]
            ?.let { associatedPropertyName ->
                storeEffectPropertyValueToSagaId[
                    SagaEffectAssociationValue(
                        sagaEffect,
                        effect.javaClass
                            .getDeclaredField(associatedPropertyName)
                            .also { it.isAccessible = true }
                            .get(effect) as UUID
                    )]
            }
    }

    private data class SagaEffect(
        val sagaName: String,
        val effectKClass: KClass<*>
    )

    private data class SagaEffectAssociationValue(
        val sagaEffect: SagaEffect,
        val associatedPropertyValue: UUID
    )
}
