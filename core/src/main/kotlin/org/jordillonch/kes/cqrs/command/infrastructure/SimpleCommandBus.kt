package org.jordillonch.kes.cqrs.command.infrastructure

import org.jordillonch.kes.cqrs.bus.domain.Command
import org.jordillonch.kes.cqrs.command.domain.CommandBus
import org.jordillonch.kes.cqrs.command.domain.CommandHandler
import org.jordillonch.kes.cqrs.command.domain.NoCommandHandlerFoundException
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect

class SimpleCommandBus : CommandBus {
    private val handlers: MutableMap<String, (Command) -> Unit> = mutableMapOf()

    override fun <C : Command> registerHandler(handler: CommandHandler<C>) {
//        @Suppress("UNCHECKED_CAST")
//        handlers[classFrom(handler)] = { command: Command -> handler.on(command as C) }
    }

    override fun <C : Command> registerHandler(handler: (C) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        handlers[classFrom(handler)] = handler as (Command) -> Unit
    }

    override fun <C : Command> registerHandler(command: Class<*>, handler: (C) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        handlers[command.canonicalName] = handler as (Command) -> Unit
    }

    override fun handle(command: Command) {
        @Suppress("UNCHECKED_CAST")
        return handlers[command::class.qualifiedName]
            ?.invoke(command)
            ?: throw NoCommandHandlerFoundException(command)
    }

    private fun <C : Command> classFrom(handler: (C) -> Unit) =
        handler.reflect()!!.parameters.first().type.toString()

    private fun <C : Command> classFrom(handler: CommandHandler<C>) =
        handler.javaClass.kotlin
            .declaredFunctions
            .firstFunctionNamedOn()
            .mapParameterTypes()
            .first { it.isSubclassOf(Command::class) }
            .qualifiedName!!

    private fun Collection<KFunction<*>>.firstFunctionNamedOn() = first { it.name == "on" }

    private fun KFunction<*>.mapParameterTypes() = parameters.map { it.type.jvmErasure }
}
