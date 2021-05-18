package tfar.speedrunnervshunter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class Utils {

    //crouching sends random disasters to hunters
    public static void randomDisasterToHunters(PlayerEntity speedrunner) {
        List<ServerPlayerEntity> players = speedrunner.getServer().getPlayerList().getPlayers();
        for (ServerPlayerEntity player : players) {
            if (speedrunner != player) {
                try {
                    Class<?> clazz = Class.forName("tfar.naturaldisasters.NaturalDisasters");
                    Method method = clazz.getMethod("toggleRandomDisaster",PlayerEntity.class);
                    method.invoke(null,player);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
