package tfar.speedrunnervshunter.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import tfar.speedrunnervshunter.SpeedrunnerVsHunter;

public class MainCommand {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
        LiteralArgumentBuilder.<CommandSourceStack>literal(SpeedrunnerVsHunter.MODID)
                .then(SpeedrunnerCommand.register()));
    }
}
