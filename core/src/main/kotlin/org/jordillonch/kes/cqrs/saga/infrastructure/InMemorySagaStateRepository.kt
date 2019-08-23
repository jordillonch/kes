package org.jordillonch.kes.cqrs.saga.infrastructure

import org.jordillonch.kes.cqrs.command.domain.Command
import org.jordillonch.kes.cqrs.event.domain.Event
import org.jordillonch.kes.cqrs.saga.domain.SagaStateRepository
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class InMemorySagaStateRepository : SagaStateRepository {
    private val storeEffectClassToPropertyName = mutableMapOf<SagaEffect, String>()
    private val storeEffectPropertyValueToSagaId = mutableMapOf<SagaEffectAssociationValue, Any>()
    private val storeSagaState = mutableMapOf<UUID, Map<String, Any>>()

    override fun associate(
        sagaId: UUID,
        sagaName: String,
        effectKClass: KClass<*>,
        associatedPropertyName: String,
        associatedPropertyValue: UUID
    ) {
        storeEffectClassToPropertyName[SagaEffect(sagaName, effectKClass)] = associatedPropertyName
        storeEffectPropertyValueToSagaId[
            SagaEffectAssociationValue(
                SagaEffect(sagaName, effectKClass),
                associatedPropertyValue
            )] = sagaId
    }

    override fun save(sagaId: UUID, state: Map<String, Any>) {
        storeSagaState[sagaId] = state
    }

    override fun find(sagaName: String, command: Command): Map<String, Any>? = findAny(sagaName, command)

    override fun find(sagaName: String, event: Event): Map<String, Any>? = findAny(sagaName, event)

    private fun findAny(sagaName: String, effect: Any): Map<String, Any>? {
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
            ?.let { sagaId -> storeSagaState[sagaId] }
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
