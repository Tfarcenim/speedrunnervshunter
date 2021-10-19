package tfar.speedrunnervshunter;

import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class GameManager {


    static final Random rand = new Random();
    public static boolean active;
    public static UUID speedrunnerID;
    public static List<BlockPos> TROPHY_LOCATIONS = new ArrayList<>();
    public static ForgeConfigSpec.BooleanValue HUNTERS_BLIND;
    public static BlockPos activeBlock;
    static ResourceKey<Level> levelKey;
    public static int trophyCount;

    public static void start(MinecraftServer server, int distance, ServerPlayer speedrunner, int count) {

        // bossInfo = new ServerBossInfo(new TextComponent("Timer"), BossInfo.Color.WHITE, BossInfo.Overlay.PROGRESS);
        // bossInfo.setPercent(1);

        speedrunnerID = speedrunner.getGameProfile().getId();
        levelKey = speedrunner.level.dimension();
        TROPHY_LOCATIONS.clear();
        giveItems(server);
        trophyCount = count;
        setupTrophies(server, distance, speedrunner, count);
        active = true;
    }

    private static void giveItems(MinecraftServer server) {
        for (ServerPlayer playerMP : server.getPlayerList().getPlayers()) {
            ItemStack stack = new ItemStack(Items.COMPASS);
            if (!isSpeedrunner(playerMP)) {
                stack.setHoverName(new TextComponent("Hunter's Compass"));
            } else {
                stack.setHoverName(new TextComponent("Speedrunner's Compass"));
                playerMP.addItem(new ItemStack(ModProxy.WRENCH));
            }
            playerMP.addItem(stack);
        }
    }

    public static void stopCommand(CommandSourceStack sourceStack) {
        if (active) {
            stop(sourceStack.getServer(), Resolution.STOPPED_BY_COMMAND);
        } else {
            sourceStack.sendFailure(new TextComponent("Can't stop a game that never started"));
        }
    }

    public static void stop(MinecraftServer server, Resolution resolution) {
        //bossInfo.removeAllPlayers();

        TROPHY_LOCATIONS.clear();
        if (resolution == Resolution.WIN) {
            server.getPlayerList().broadcastMessage(new TranslatableComponent("text.speedrunnervshunter.speedrunner_win"), ChatType.CHAT, Util.NIL_UUID);
        } else if (resolution == Resolution.STOPPED_BY_COMMAND) {
            server.getPlayerList().broadcastMessage(new TranslatableComponent("Game Stopped"), ChatType.CHAT, Util.NIL_UUID);
        }

        server.getLevel(levelKey).setBlockAndUpdate(activeBlock, Blocks.AIR.defaultBlockState());
        activeBlock = null;
        speedrunnerID = null;
        SpeedrunnerVsHunter.INDEX = 0;
        active = false;
    }

    static boolean isSpeedrunner(Player player) {
        return player.getGameProfile().getId().equals(speedrunnerID);
    }

    private static void setupTrophies(MinecraftServer server, int distance, Player speedrunner, int count) {
        BlockPos center = speedrunner.blockPosition();

        double rot = rand.nextInt(360);

        for (int i = 0; i < count; i++) {
            double offset = i * 360d / count;
            int x = (int) (center.getX() + distance * Math.cos((Math.PI / 180) * (rot + offset)));
            int z = (int) (center.getZ() + distance * Math.sin((Math.PI / 180) * (rot + offset)));

            int y = server.getLevel(levelKey).getChunk(x >> 4, z >> 4)
                    .getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;         
            
            BlockPos trophyLocation = new BlockPos(x,y, z);
            TROPHY_LOCATIONS.add(trophyLocation);
        }


      /*  for (TrophyLocation location : TROPHY_LOCATIONS) {
            int y = world.getChunk(location.getPos().getX() >> 4, z >> 4)
                    .getHeight(Heightmap.Types.MOTION_BLOCKING, location.getPos().getX() & 15, z & 15) + 1;
            location.setY(y);
            world.setBlockAndUpdate(location.getPos(), Blocks.GOLD_BLOCK.defaultBlockState());
        }*/

        placeNextTrophy(server);
    }

    public static void placeNextTrophy(MinecraftServer server) {
        System.out.println("trophy placed");
        BlockPos location = TROPHY_LOCATIONS.get(rand.nextInt(TROPHY_LOCATIONS.size()));

        server.getLevel(levelKey).setBlockAndUpdate(location, Blocks.GOLD_BLOCK.defaultBlockState());

        activeBlock = location;
    }
}
