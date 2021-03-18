package tfar.speedrunnervshunter.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import tfar.speedrunnervshunter.SpeedrunnerVsHunter;

import java.util.Collection;

public class SpeedrunnerCommand {

    static ArgumentBuilder<CommandSource, ?> register()
    {
        return Commands.literal("start")
                .requires(cs->cs.hasPermissionLevel(2)) //permission
                .then(Commands.argument("speedrunner", GameProfileArgument.gameProfile())
                        .then(Commands.argument("distance", IntegerArgumentType.integer(0))
                                .executes(ctx -> execute(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "speedrunner"),
                                IntegerArgumentType.getInteger(ctx,"distance")
                        )
                )));
    }

    public static int execute(CommandSource source, Collection<GameProfile> target, int distance) throws CommandException {
            MinecraftServer server = source.getServer();
        ServerPlayerEntity speedrunner = null;
            for (GameProfile gameProfile : target) {
                speedrunner = server.getPlayerList().getPlayerByUUID(gameProfile.getId());
            }

                TranslationTextComponent translationTextComponent =
                        new TranslationTextComponent("commands.speedrunnervshunter.speedrunner.success",speedrunner.getDisplayName());
                source.sendFeedback(translationTextComponent,true);
                SpeedrunnerVsHunter.start(source.getServer(),distance,speedrunner);
        return 1;
    }
}
