package tfar.speedrunnervshunter;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;

import net.minecraft.network.play.server.SSpawnPositionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.PacketDistributor;
import tfar.speedrunnervshunter.commands.MainCommand;
import tfar.speedrunnervshunter.network.PacketHandler;
import tfar.speedrunnervshunter.network.S2CAddTrophyPosPacket;
import tfar.speedrunnervshunter.network.S2CRemoveTrophyPosPacket;

import java.util.*;

@Mod(SpeedrunnerVsHunter.MODID)
@Mod.EventBusSubscriber
public class SpeedrunnerVsHunter {
    public static final String MODID = "speedrunnervshunter";

    public static ServerPlayerEntity speedrunner;
    public static List<TrophyLocation> TROPHY_LOCATIONS = new ArrayList<>();

    public SpeedrunnerVsHunter() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::common);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.addListener(Client::renderLast);
            MinecraftForge.EVENT_BUS.addListener(Client::logout);

        }
    }

    public void common(FMLCommonSetupEvent event) {
        PacketHandler.registerMessages(MODID);
    }

    public void onServerStart(FMLServerStartingEvent e) {
        MainCommand.registerCommands(e.getCommandDispatcher());
    }

    private static final CompoundNBT hunter_compound = new CompoundNBT();
    private static final CompoundNBT speedrunner_compound = new CompoundNBT();

    private static final String HUNTER_KEY = "speedrunnervshunter:hunter";
    private static final String SPEEDRUNNER_KEY = "speedrunnervshunter:speedrunner";

    public static int TROPHIES = 3;

    static {
        hunter_compound.putBoolean(HUNTER_KEY, true);
        speedrunner_compound.putBoolean(SPEEDRUNNER_KEY, true);
    }

    private static final Random rand = new Random();

    public static void start(MinecraftServer server, int distance) {
        TROPHY_LOCATIONS.clear();
        for (ServerPlayerEntity playerMP : server.getPlayerList().getPlayers()) {
            ItemStack stack = new ItemStack(Items.COMPASS);
            if (playerMP != speedrunner) {
                stack.setDisplayName(new StringTextComponent("Hunter's Compass"));
                stack.getTag().putBoolean(HUNTER_KEY,true);
            } else {
                stack.setDisplayName(new StringTextComponent("Speedrunner's Compass"));
                stack.getTag().putBoolean(SPEEDRUNNER_KEY,true);
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

        ServerWorld world = server.getWorld(DimensionType.OVERWORLD);


        for (TrophyLocation location : TROPHY_LOCATIONS) {
            int y = world.getChunk(location.getPos().getX() >> 4, location.getPos().getZ() >> 4)
                    .getTopBlockY(Heightmap.Type.MOTION_BLOCKING,location.getPos().getX()/16, location.getPos().getZ()/16) + 1;
            location.setY(y);
            world.setBlockState(location.getPos(), Blocks.GOLD_BLOCK.getDefaultState());
            PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> world.getDimension().getType()),new S2CAddTrophyPosPacket(location.getPos()));
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.START && !e.player.world.isRemote && speedrunner != null) {
            BlockPos nearest;
            if (e.player == speedrunner) {
                nearest = findNearestTrophy((ServerPlayerEntity) e.player);
            } else {
                nearest = new BlockPos(speedrunner);
            }
            ((ServerPlayerEntity) e.player).connection.sendPacket(new SSpawnPositionPacket(nearest));
        }
    }


    /*@SubscribeEvent
    public static void loadChunk(ChunkEvent.Load e) {
        World world = (World) e.getWorld();
        if (!world.isRemote) {
            for (TrophyLocation pos : TROPHY_LOCATIONS) {
                if (e.getChunk().getPos().equals(new ChunkPos(pos.getPos())) && !pos.isGenerated()) {
                    int y = world.getHeight(Heightmap.Type.MOTION_BLOCKING,pos.getPos().getX(), pos.getPos().getZ());
                    pos.setY(y);
                    world.setBlockState(pos.getPos(), Blocks.GOLD_BLOCK.getDefaultState());
                }
            }
        }
    }*/

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        PlayerEntity player = event.getPlayer();
        for (Iterator<TrophyLocation> iterator = TROPHY_LOCATIONS.iterator(); iterator.hasNext(); ) {
            TrophyLocation trophyLocation = iterator.next();
            if (trophyLocation.getPos().equals(pos)) {
                World world = player.world;
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> world.getDimension().getType()),new S2CRemoveTrophyPosPacket(pos));
                player.sendMessage(new TranslationTextComponent("text.speedrunnervshunter.trophy_get"));
                iterator.remove();
            }
        }

        if (TROPHY_LOCATIONS.isEmpty() && speedrunner != null) {
            stop();
        }
    }

    private static BlockPos findNearestTrophy(ServerPlayerEntity playerMP) {
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
        server.getPlayerList().sendMessage(new TranslationTextComponent("text.speedrunnervshunter.speedrunner_win"));
        speedrunner = null;
    }
}
