package org.jordillonch.kes.cqrs.command.domain

import org.jordillonch.kes.cqrs.Effect

//interface Command : Effect

interface CommandHandler<in C : Command>: EffectHandler

interface CommandBus {
    fun <C : Command> registerHandler(handler: CommandHandler<C>)
    fun <C : Command> registerHandler(handler: (C) -> Unit)
    fun <C : Command> registerHandler(command: Class<*>, handler: (C) -> Unit)
    fun handle(command: Command)
}

class NoCommandHandlerFoundException(val command: Command) : RuntimeException()
