package tfar.speedrunnervshunter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSpawnPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import tfar.speedrunnervshunter.commands.MainCommand;

import java.util.*;

@Mod(modid = SpeedrunnerVsHunter.MODID, name = SpeedrunnerVsHunter.NAME, version = SpeedrunnerVsHunter.VERSION, acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber
public class SpeedrunnerVsHunter {
    public static final String MODID = "speedrunnervshunter";
    public static final String NAME = "Speedrunner vs Hunter";
    public static final String VERSION = "1.0";

    public static EntityPlayerMP speedrunner;
    public static List<TrophyLocation> TROPHY_LOCATIONS = new ArrayList<>();


    @Mod.EventHandler
    public void onServerStart(FMLServerStartingEvent e) {
        e.registerServerCommand(new MainCommand());
    }

    private static final NBTTagCompound hunter_compound = new NBTTagCompound();
    private static final NBTTagCompound speedrunner_compound = new NBTTagCompound();

    private static final String HUNTER_KEY = "speedrunnervshunter:hunter";
    private static final String SPEEDRUNNER_KEY = "speedrunnervshunter:speedrunner";

    public static int TROPHIES = 3;

    static {
        hunter_compound.setBoolean(HUNTER_KEY, true);
        speedrunner_compound.setBoolean(SPEEDRUNNER_KEY, true);
    }

    private static final Random rand = new Random();

    public static void start(MinecraftServer server, int distance) {
        TROPHY_LOCATIONS.clear();
        for (EntityPlayerMP playerMP : server.getPlayerList().getPlayers()) {
            ItemStack stack = new ItemStack(Items.COMPASS);
            stack.setTagCompound(new NBTTagCompound());
            if (playerMP != speedrunner) {
                stack.setStackDisplayName("Hunter's Compass");
                stack.getTagCompound().setBoolean(HUNTER_KEY,true);
            } else {
                stack.setStackDisplayName("Speedrunner's Compass");
                stack.getTagCompound().setBoolean(SPEEDRUNNER_KEY,true);
            }
            playerMP.addItemStackToInventory(stack);
        }

        BlockPos center = speedrunner.getPosition();

        double rot = rand.nextInt(360);

        for (int i = 0; i < TROPHIES; i++) {
            double offset = i * 360d / TROPHIES;
            int x = (int) (center.getX() + distance * Math.cos((Math.PI / 180) * (rot + offset)));
            int z = (int) (center.getZ() + distance * Math.sin((Math.PI / 180) * (rot + offset)));
            TrophyLocation trophyLocation = new TrophyLocation(x, z);
            TROPHY_LOCATIONS.add(trophyLocation);
        }

        WorldServer world = server.getWorld(0);

        Set<ChunkPos> loaded = world.entitySpawner.eligibleChunksForSpawning;

        for (TrophyLocation location : TROPHY_LOCATIONS) {
            ChunkPos chunkPos = new ChunkPos(location.getPos());
            if (loaded.contains(chunkPos)) {
                int y = world.getHeight(location.getPos().getX(), location.getPos().getZ());
                location.setY(y);
                world.setBlockState(location.getPos(), Blocks.GOLD_BLOCK.getDefaultState());
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.START && !e.player.world.isRemote && speedrunner != null) {
            BlockPos nearest;
            if (e.player == speedrunner) {
                nearest = findNearestTrophy((EntityPlayerMP) e.player);
            } else {
                nearest = new BlockPos(speedrunner);
            }
            ((EntityPlayerMP) e.player).connection.sendPacket(new SPacketSpawnPosition(nearest));
        }
    }


    @SubscribeEvent
    public static void loadChunk(ChunkEvent.Load e) {
        World world = e.getWorld();
        if (!world.isRemote) {
            for (TrophyLocation pos : TROPHY_LOCATIONS) {
                if (e.getChunk().getPos().equals(new ChunkPos(pos.getPos())) && !pos.isGenerated()) {
                    int y = world.getHeight(pos.getPos().getX(), pos.getPos().getZ());
                    pos.setY(y);
                    world.setBlockState(pos.getPos(), Blocks.GOLD_BLOCK.getDefaultState());
                }
            }
        }
    }

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        EntityPlayer player = event.getPlayer();
        for (Iterator<TrophyLocation> iterator = TROPHY_LOCATIONS.iterator(); iterator.hasNext(); ) {
            TrophyLocation trophyLocation = iterator.next();
            if (trophyLocation.getPos().equals(pos)) {
                player.sendMessage(new TextComponentTranslation("text.speedrunnervshunter.trophy_get"));
                iterator.remove();
            }
        }

        if (TROPHY_LOCATIONS.isEmpty() && speedrunner != null) {
            stop();
        }
    }

    private static BlockPos findNearestTrophy(EntityPlayerMP playerMP) {
        double dist = Double.MAX_VALUE;
        BlockPos near = null;
        BlockPos playerPos = playerMP.getPosition();
        for (TrophyLocation pos : TROPHY_LOCATIONS) {
            double dist1 = playerPos.distanceSq(pos.getPos());
            if (dist1 < dist) {
                dist = dist1;
                near = pos.getPos();
            }
        }
        return near;
    }

    public static void stop() {
        MinecraftServer server = speedrunner.getServer();
        server.getPlayerList().sendMessage(new TextComponentTranslation("text.speedrunnervshunter.speedrunner_win"));
        speedrunner = null;
    }
}
