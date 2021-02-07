package tfar.speedrunnervshunter.commands;

import net.minecraft.command.ICommandSender;
import net.minecraftforge.server.command.CommandTreeBase;
import tfar.speedrunnervshunter.SpeedrunnerVsHunter;

public class MainCommand extends CommandTreeBase {

    public MainCommand() {
        addSubcommand(new SpeedrunnerCommand());
    }

    @Override
    public String getName() {
        return SpeedrunnerVsHunter.MODID;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands."+ SpeedrunnerVsHunter.MODID +".usage";
    }
}
