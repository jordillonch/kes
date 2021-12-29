package org.jordillonch.kes.cqrs.command.domain

import org.jordillonch.kes.cqrs.Effect

interface Command : Effect
interface Event : Effect

interface EffectHandler

interface Bus {
    fun register(handler: EffectHandler)
    fun push(effect: Effect)
    fun push(effects: List<Effect>)
    fun drain()
}

data class EntityCreated(val entity: Any): Event
data class EntityUpdated(val entity: Any): Event
data class EntityDeleted(val entity: Any): Event
