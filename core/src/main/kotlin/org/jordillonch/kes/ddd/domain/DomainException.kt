package org.jordillonch.kes.ddd.domain

abstract class DomainException(
        val errorCode: String,
        val errorMessage: String,
        val exception: Throwable? = null) : RuntimeException(errorMessage, exception)