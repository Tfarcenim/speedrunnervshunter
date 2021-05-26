package tfar.speedrunnervshunter;

import me.ichun.mods.morph.api.MorphApi;
import net.minecraft.entity.player.EntityPlayerMP;

public class ModProxy {

    //acquire morphs when game starts
    public static void grantHunterMorphsTo(EntityPlayerMP speedrunner) {
        for (EntityPlayerMP playerMP : speedrunner.getServer().getPlayerList().getPlayers()) {
            MorphApi.getApiImpl().acquireMorph(speedrunner,playerMP,false,false);
        }
    }
}
