package org.jordillonch.kes.cqrs.unit_of_work.infrastructure

import javax.transaction.Status
import javax.transaction.Synchronization
import javax.transaction.Transaction
import javax.transaction.xa.XAResource

class SimpleTransaction : Transaction {
    override fun getStatus() = Status.STATUS_ACTIVE

    override fun commit() {
    }

    override fun registerSynchronization(sync: Synchronization?) {
        TODO("not implemented")
    }

    override fun setRollbackOnly() {
        TODO("not implemented")
    }

    override fun rollback() {
        TODO("not implemented")
    }

    override fun enlistResource(xaRes: XAResource?): Boolean {
        TODO("not implemented")
    }

    override fun delistResource(xaRes: XAResource?, flag: Int): Boolean {
        TODO("not implemented")
    }
}
