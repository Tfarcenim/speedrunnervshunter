package tfar.speedrunnervshunter;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class Utils {

    //crouching sends random disasters to hunters
    public static void randomDisasterToHunters(Player speedrunner) {
        List<ServerPlayer> players = speedrunner.getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            if (speedrunner != player) {
                try {
                    Class<?> clazz = Class.forName("tfar.naturaldisasters.NaturalDisasters");
                    Method method = clazz.getMethod("toggleRandomDisaster", Player.class);
                    method.invoke(null,player);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
