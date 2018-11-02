package org.jordillonch.kes.faker

import com.github.javafaker.Faker as JavaFaker

object Faker {
    private val faker = JavaFaker()

    fun instance(): JavaFaker = faker
}