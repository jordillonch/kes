package org.jordillonch.kes.cqrs.unit_of_work.infrastructure

import javax.transaction.Status
import javax.transaction.Transaction
import javax.transaction.TransactionManager

class SimpleTransactionManager : TransactionManager {
    private var currentTransaction: Transaction? = null
    private var nestedLevel = 0

    override fun getTransaction() =
        (currentTransaction ?: SimpleTransaction().also { currentTransaction = it })

    override fun begin() {
        transaction
        nestedLevel++
    }

    override fun commit() {
        checkNotNull(currentTransaction) { "Transaction not found" }

        if (--nestedLevel == 0) {
            currentTransaction!!.commit()
            currentTransaction = null
        }
    }

    override fun getStatus(): Int {
        return when(true) {
            currentTransaction == null -> Status.STATUS_NO_TRANSACTION
            nestedLevel > 0 -> Status.STATUS_ACTIVE
            else -> Status.STATUS_PREPARED
        }
    }

    override fun rollback() {
        TODO("not implemented")
    }

    override fun setRollbackOnly() {
        TODO("not implemented")
    }

    override fun suspend(): Transaction {
        TODO("not implemented")
    }

    override fun resume(tobj: Transaction?) {
        TODO("not implemented")
    }

    override fun setTransactionTimeout(seconds: Int) {
        TODO("not implemented")
    }
}
