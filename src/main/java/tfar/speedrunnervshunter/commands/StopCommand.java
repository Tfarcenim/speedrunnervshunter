package tfar.speedrunnervshunter.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import tfar.speedrunnervshunter.GameManager;

public class StopCommand {

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("stop")
                .requires(cs -> cs.hasPermission(2)) //permission
                                .executes(ctx -> execute(ctx.getSource()));
    }

    public static int execute(CommandSourceStack source) {
        GameManager.stopCommand(source);
        return 1;
    }
}
