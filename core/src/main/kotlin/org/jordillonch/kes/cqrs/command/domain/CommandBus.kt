package org.jordillonch.kes.cqrs.command.domain

interface Command

interface CommandHandler<in C : Command> {
    fun on(command: C)
}

interface CommandBus {
    fun <C : Command> registerHandler(handler: CommandHandler<C>)
    fun <C : Command> registerHandler(handler: (C) -> Unit)
    fun <C : Command> registerHandler(command: Class<*>, handler: (C) -> Unit)
    fun handle(command: Command)
}

class NoCommandHandlerFoundException : RuntimeException()
