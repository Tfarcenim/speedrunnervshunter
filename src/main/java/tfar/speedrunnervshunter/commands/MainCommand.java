package tfar.speedrunnervshunter.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import tfar.speedrunnervshunter.SpeedrunnerVsHunter;

public class MainCommand {

    public static void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
        LiteralArgumentBuilder.<CommandSource>literal(SpeedrunnerVsHunter.MODID)
                .then(SpeedrunnerCommand.register()));
    }
}
