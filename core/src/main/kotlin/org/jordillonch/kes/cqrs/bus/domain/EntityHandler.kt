package org.jordillonch.kes.cqrs.bus.domain

interface EntityHandler {
    fun newInstance(): EntityHandler
}
