package tfar.speedrunnervshunter.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import tfar.speedrunnervshunter.SpeedrunnerVsHunter;

import java.util.Collection;

public class SpeedrunnerCommand {

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("start")
                .requires(cs -> cs.hasPermission(2)) //permission
                .then(Commands.argument("speedrunner", GameProfileArgument.gameProfile())
                        .then(Commands.argument("distance", IntegerArgumentType.integer(0))
                                .executes(ctx -> execute(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "speedrunner"),
                                        IntegerArgumentType.getInteger(ctx, "distance")
                                        )
                                )));
    }

    public static int execute(CommandSourceStack source, Collection<GameProfile> target, int distance) {
        MinecraftServer server = source.getServer();
        ServerPlayer speedrunner = null;
        for (GameProfile gameProfile : target) {
            speedrunner = server.getPlayerList().getPlayer(gameProfile.getId());
        }

        TranslatableComponent translationTextComponent =
                new TranslatableComponent("commands.speedrunnervshunter.speedrunner.success", speedrunner.getDisplayName());
        source.sendSuccess(translationTextComponent, true);
        SpeedrunnerVsHunter.start(source.getServer(), distance, speedrunner);
        return 1;
    }
}
