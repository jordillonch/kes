package org.jordillonch.kes.cqrs.bus.domain.association

import org.jordillonch.kes.cqrs.bus.domain.Effect
import kotlin.reflect.KProperty1

data class Associate(
    val handler: Any,
    val entityId: Any,
    val associatedField: KProperty1<*, *>,
    val associatedId: Any,
): Effect
