package tfar.speedrunnervshunter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class Hooks {

    public static void onBlockDrop(Entity parameter, BlockPos pos) {
        if (parameter instanceof ServerPlayer player && GameManager.active && GameManager.TROPHY_LOCATIONS.contains(pos)) {

            List<ServerPlayer> players = player.getServer().getPlayerList().getPlayers();
            int trophyNumber = GameManager.trophyCount - GameManager.TROPHY_LOCATIONS.size() + 1;

            for (ServerPlayer player1 : players) {
                player1.connection.send(new ClientboundSetTitleTextPacket(new TranslatableComponent("text.speedrunnervshunter.trophy_get")));
                player1.connection.send(new ClientboundSetSubtitleTextPacket(new TextComponent((GameManager.trophyCount - trophyNumber) +
                        "/" + GameManager.trophyCount + " TROPHIES REMAIN")));
            }
            //player.sendMessage(new TranslatableComponent("text.speedrunnervshunter.trophy_get"), Util.NIL_UUID);
            SpeedrunnerVsHunter.handleSpecialDrops(player);
            GameManager.TROPHY_LOCATIONS.remove(pos);
            if (GameManager.TROPHY_LOCATIONS.isEmpty()) {
                GameManager.stop(player.getServer(), Resolution.WIN);
            } else {
                GameManager.placeNextTrophy(player.getServer());
            }
        }
    }
}
