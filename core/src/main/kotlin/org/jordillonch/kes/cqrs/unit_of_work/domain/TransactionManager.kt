package org.jordillonch.kes.cqrs.unit_of_work.domain

import org.jordillonch.kes.cqrs.unit_of_work.domain.TransactionManager.State.*

class TransactionManager {
    private var state = IDLE
    private var nestedLevel = 0

    @Synchronized
    fun beginTransaction(): Boolean {
        return if (state == IDLE) {
            state = BUSY
            nestedLevel++
            true
        } else {
            false
        }
    }

    @Synchronized
    fun commit() {
        nestedLevel--
        if (nestedLevel == 0) state = IDLE
    }

    fun isBusy() = state == BUSY

    private enum class State {
        IDLE, BUSY
    }
}
