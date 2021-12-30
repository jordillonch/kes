package org.jordillonch.kes.cqrs.bus.domain.saga

import org.jordillonch.kes.cqrs.bus.domain.entity.EntityCreated
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityDeleted
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityHandler
import org.jordillonch.kes.cqrs.bus.domain.entity.EntityUpdated

typealias SagaHandler = EntityHandler

typealias SagaStateCreated = EntityCreated
typealias SagaStateUpdated = EntityUpdated
typealias SagaStateDeleted = EntityDeleted
