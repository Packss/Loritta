package net.perfectdreams.loritta.interactions.commands.vanilla

import net.perfectdreams.loritta.api.commands.CommandContext
import net.perfectdreams.loritta.api.commands.LorittaCommand

class PingAnotherThingCommand : LorittaCommand<CommandContext>(PingCommandDeclaration.AnotherThing, PingCommandDeclaration) {
    override suspend fun executes(context: CommandContext) {
        context.sendMessage("Pong???")
    }
}