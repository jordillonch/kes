package org.jordillonch.kes.cqrs.bus.domain.entity

import kotlin.reflect.KFunction1
import kotlin.reflect.full.memberProperties

interface IdentifiedEntity {
    fun primaryId(): Any
}

inline fun <reified I: Any, O>I.evolveTo(nextType: KFunction1<*, O>): O = with(nextType) {
    val propertiesByName = I::class.memberProperties.associateBy { it.name }
    callBy(parameters.associate { parameter ->
        parameter to propertiesByName[parameter.name]?.get(this@evolveTo)
    })
}
