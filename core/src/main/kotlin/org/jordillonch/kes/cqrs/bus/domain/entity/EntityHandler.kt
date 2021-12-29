package org.jordillonch.kes.cqrs.bus.domain.entity

interface EntityHandler {
    fun newInstance(): EntityHandler
}
