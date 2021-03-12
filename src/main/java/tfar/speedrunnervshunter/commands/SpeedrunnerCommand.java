package tfar.speedrunnervshunter.commands;

import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import tfar.speedrunnervshunter.SpeedrunnerVsHunter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class SpeedrunnerCommand extends CommandBase {

    private final String s = "commands." + SpeedrunnerVsHunter.MODID + "." + getName();

    @Override
    public String getName() {
        return "speedrunner";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return s + ".usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 2 && args[0].length() > 0) {
            GameProfile gameprofile = server.getPlayerProfileCache().getGameProfileForUsername(args[0]);
            int distance = Integer.parseInt(args[1]);
            if (gameprofile == null) {
                throw new CommandException(s + ".failed", args[0]);
            } else {
                EntityPlayerMP playerMP = server.getPlayerList().getPlayerByUsername(args[0]);
                server.getPlayerList().sendMessage(new TextComponentTranslation("commands.speedrunnervshunter.speedrunner.success",args[0]));
                SpeedrunnerVsHunter.start(server,distance,playerMP);
            }
        } else {
            throw new WrongUsageException(s + ".usage");
        }
    }

    /**
     * Get a list of options for when the user presses the TAB key
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        return Collections.emptyList();
    }
}
