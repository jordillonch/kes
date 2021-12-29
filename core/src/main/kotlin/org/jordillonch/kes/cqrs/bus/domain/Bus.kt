package org.jordillonch.kes.cqrs.bus.domain

import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandler
import org.jordillonch.kes.cqrs.bus.domain.entity.Repository
import kotlin.reflect.KClass

interface Bus {
    fun register(handler: EffectsHandler)
    fun <E, I> register(
        handler: KClass<out EntityHandler>,
        instanceCreator: () -> EntityHandler,
        repository: Repository<E, I>
    )
    fun push(effect: Effect)
    fun push(effects: List<Effect>)
    fun drain()
}

typealias Handler = (Effect) -> List<Effect>
