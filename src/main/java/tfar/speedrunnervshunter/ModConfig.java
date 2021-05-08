package tfar.speedrunnervshunter;

import net.minecraftforge.common.config.Config;

@Config(modid = SpeedrunnerVsHunter.MODID)
public class ModConfig {

    @Config.Name("trophy_count")
    public static int trophy_count = 3;

    @Config.Name("infinity_gauntlet_mode")
    public static boolean infinity_gauntlet_mode = false;

}
