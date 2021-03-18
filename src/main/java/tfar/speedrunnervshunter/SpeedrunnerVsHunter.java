package tfar.speedrunnervshunter;

import com.google.common.collect.Lists;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;

import net.minecraft.network.play.server.SWorldSpawnChangedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import tfar.speedrunnervshunter.commands.MainCommand;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(SpeedrunnerVsHunter.MODID)
@Mod.EventBusSubscriber
public class SpeedrunnerVsHunter {
    public static final String MODID = "speedrunnervshunter";

    public static UUID speedrunnerID;
    public static List<TrophyLocation> TROPHY_LOCATIONS = new ArrayList<>();

    public SpeedrunnerVsHunter() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
    }

    public void onServerStart(RegisterCommandsEvent e) {
        MainCommand.registerCommands(e.getDispatcher());
    }

    public static int TROPHIES = 3;

    private static final Random rand = new Random();

    public static void start(MinecraftServer server, int distance, ServerPlayerEntity speedrunner) {
        speedrunnerID = speedrunner.getGameProfile().getId();
        TROPHY_LOCATIONS.clear();
        for (ServerPlayerEntity playerMP : server.getPlayerList().getPlayers()) {
            ItemStack stack = new ItemStack(Items.COMPASS);
            if (!isSpeedrunner(playerMP)) {
                stack.setDisplayName(new StringTextComponent("Hunter's Compass"));
            } else {
                stack.setDisplayName(new StringTextComponent("Speedrunner's Compass"));
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

        ServerWorld world = server.getWorld(World.OVERWORLD);


        for (TrophyLocation location : TROPHY_LOCATIONS) {
            int y = world.getChunk(location.getPos().getX() >> 4, location.getPos().getZ() >> 4)
                    .getTopBlockY(Heightmap.Type.MOTION_BLOCKING, location.getPos().getX() & 15, location.getPos().getZ() & 15) + 1;
            location.setY(y);
            world.setBlockState(location.getPos(), Blocks.GOLD_BLOCK.getDefaultState());
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.START && !e.player.world.isRemote && speedrunnerID != null) {
            BlockPos nearest;
            if (isSpeedrunner(e.player)) {
                nearest = findNearestTrophy((ServerPlayerEntity) e.player);
            } else {
                nearest = new BlockPos(e.player.getServer().getPlayerList().getPlayerByUUID(speedrunnerID).getPosition());
            }
            ((ServerPlayerEntity) e.player).connection.sendPacket(new SWorldSpawnChangedPacket(nearest, ((ServerPlayerEntity) e.player).getServerWorld().func_242107_v()));
        }
    }

    private static boolean isSpeedrunner(PlayerEntity player) {
        return player.getGameProfile().getId().equals(speedrunnerID);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (speedrunnerID != null && event.phase == TickEvent.Phase.START) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            long time = server.getWorld(World.OVERWORLD).getGameTime();
            if (time % 1200 == 0) {
                List<ServerPlayerEntity> players = Lists.newArrayList(server.getPlayerList().getPlayers());
                players.removeIf(serverPlayerEntity -> serverPlayerEntity.getGameProfile().getId().equals(speedrunnerID));
                if (!players.isEmpty()) {
                    ServerPlayerEntity newSpeedrunner = players.get(rand.nextInt(players.size()));
                    UUID oldSpeedrunnerID = speedrunnerID;
                    ServerPlayerEntity oldspeedrunner = server.getPlayerList().getPlayerByUUID(oldSpeedrunnerID);
                    speedrunnerID = newSpeedrunner.getGameProfile().getId();

                    PlayerInventory oldinventory = oldspeedrunner.inventory;
                    for (ItemStack stack : Stream.of(oldinventory.mainInventory, oldinventory.armorInventory, oldinventory.offHandInventory).flatMap(Collection::stream).collect(Collectors.toList())) {
                        if (stack.getItem() == Items.COMPASS) {
                            stack.setDisplayName(new StringTextComponent("Hunter's Compass"));
                        }
                    }

                    PlayerInventory newinventory = newSpeedrunner.inventory;
                    for (ItemStack stack : Stream.of(newinventory.mainInventory, newinventory.armorInventory, newinventory.offHandInventory).flatMap(Collection::stream).collect(Collectors.toList())) {
                        if (stack.getItem() == Items.COMPASS) {
                            stack.setDisplayName(new StringTextComponent("Speedrunner's Compass"));
                        }
                    }
                    TranslationTextComponent translationTextComponent =
                            new TranslationTextComponent("commands.speedrunnervshunter.speedrunner.success",newSpeedrunner.getDisplayName());
                    server.sendMessage(translationTextComponent,Util.DUMMY_UUID);
                }
            }
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
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (isSpeedrunner(event.getPlayer())) {
            stop(event.getPlayer().getServer(),true);
        }
    }

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        PlayerEntity player = event.getPlayer();
        for (Iterator<TrophyLocation> iterator = TROPHY_LOCATIONS.iterator(); iterator.hasNext(); ) {
            TrophyLocation trophyLocation = iterator.next();
            if (trophyLocation.getPos().equals(pos)) {
                player.sendMessage(new TranslationTextComponent("text.speedrunnervshunter.trophy_get"), Util.DUMMY_UUID);
                iterator.remove();
            }
        }

        if (TROPHY_LOCATIONS.isEmpty() && speedrunnerID != null) {
            stop(player.getServer(),false);
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

    public static void stop(MinecraftServer server,boolean abort) {
        if (!abort)
        server.getPlayerList().func_232641_a_(new TranslationTextComponent("text.speedrunnervshunter.speedrunner_win"), ChatType.CHAT, Util.DUMMY_UUID);
        speedrunnerID = null;
    }
}
