package tfar.speedrunnervshunter;

import com.google.common.collect.Lists;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.network.play.server.SWorldSpawnChangedPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
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

    static ForgeConfigSpec serverSpec;

    public SpeedrunnerVsHunter() {

        final Pair<SpeedrunnerVsHunter, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(this::spec);
        serverSpec = specPair.getRight();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverSpec);

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::knockback);
        MinecraftForge.EVENT_BUS.addListener(this::fallDamage);
    }

    // private static ServerBossInfo bossInfo;

    public void onServerStart(FMLServerStartingEvent e) {

    }

    public void commands(RegisterCommandsEvent e) {
        MainCommand.registerCommands(e.getDispatcher());
    }

    private static final Random rand = new Random();

    public static void start(MinecraftServer server, int distance, ServerPlayerEntity speedrunner) {

        // bossInfo = new ServerBossInfo(new StringTextComponent("Timer"), BossInfo.Color.WHITE, BossInfo.Overlay.PROGRESS);
        // bossInfo.setPercent(1);

        speedrunnerID = speedrunner.getGameProfile().getId();
        TROPHY_LOCATIONS.clear();
        for (ServerPlayerEntity playerMP : server.getPlayerList().getPlayers()) {

            //    bossInfo.addPlayer(playerMP);

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

        for (int i = 0; i < TROPHY_COUNT.get(); i++) {
            double offset = i * 360d / TROPHY_COUNT.get();
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
                if (HUNTERS_BLIND.get() && e.player.world.getGameTime() % 20 == 0) {
                    e.player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 80, 0, false, false));
                }
                nearest = new BlockPos(e.player.getServer().getPlayerList().getPlayerByUUID(speedrunnerID).getPosition());
            }
            ((ServerPlayerEntity) e.player).connection.sendPacket(new SWorldSpawnChangedPacket(nearest, ((ServerPlayerEntity) e.player).getServerWorld().func_242107_v()));
        }
    }

    private static boolean isSpeedrunner(PlayerEntity player) {
        return player.getGameProfile().getId().equals(speedrunnerID);
    }

    public static int TIME_LIMIT = 1200;

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (speedrunnerID != null && event.phase == TickEvent.Phase.START) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();


            final long scaledTime = server.getWorld(World.OVERWORLD).getGameTime() % TIME_LIMIT;
            //  bossInfo.setPercent(1 - (scaledTime / (float)TIME_LIMIT));
            if (scaledTime == TIME_LIMIT - 200) {
                final List<ServerPlayerEntity> players = server.getPlayerList().getPlayers().stream().filter(playerMP -> !(playerMP instanceof FakePlayer)).collect(Collectors.toList());
                //if (players.size() > 1) {
                //          server.getPlayerList().func_232641_a_(new StringTextComponent("Swapping in 10 seconds!"),ChatType.CHAT,Util.DUMMY_UUID);
                //  }
            }

            if (scaledTime == 0 && false) {
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
                            new TranslationTextComponent("commands.speedrunnervshunter.speedrunner.success", newSpeedrunner.getDisplayName());
                    server.getPlayerList().func_232641_a_(translationTextComponent, ChatType.CHAT, Util.DUMMY_UUID);
                }
            }
        }
    }

    public static void updateStatus(ServerPlayerEntity playerEntity) {
        if (isSpeedrunner(playerEntity)) {
            playerEntity.connection.sendPacket(new STitlePacket(STitlePacket.Type.ACTIONBAR, new StringTextComponent("You're the Speedrunner"), 0, TIME_LIMIT, 0));
        } else {
            playerEntity.connection.sendPacket(new STitlePacket(STitlePacket.Type.ACTIONBAR, new StringTextComponent("You're a Hunter"), 0, TIME_LIMIT, 0));
        }
    }

    private void knockback(LivingKnockBackEvent event) {
        if (MOBS_KNOCKBACK_10000.get()) {
            LivingEntity target = event.getEntityLiving();
            // DamageSource source = target.getLastDamageSource();
            LivingEntity attacker = target.getRevengeTarget();
            if (attacker instanceof MobEntity) {
                event.setStrength(event.getOriginalStrength() + 5000);
            }
        }
    }

    private void fallDamage(LivingHurtEvent e) {
        if (FALL_DAMAGE_HEALS_SPEEDRUNNER.get()) {
            LivingEntity living = e.getEntityLiving();
            if (e.getSource() == DamageSource.FALL && living instanceof PlayerEntity && isSpeedrunner((PlayerEntity) living)) {
                living.heal(e.getAmount());
                e.setCanceled(true);
            }
        }
    }

    public static ForgeConfigSpec.IntValue TROPHY_COUNT;
    public static ForgeConfigSpec.BooleanValue HUNTERS_BLIND;
    public static ForgeConfigSpec.BooleanValue MOBS_KNOCKBACK_10000;
    public static ForgeConfigSpec.BooleanValue FALL_DAMAGE_HEALS_SPEEDRUNNER;


    private SpeedrunnerVsHunter spec(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        TROPHY_COUNT = builder.defineInRange("trophy_count", 3, 1, 10000);
        HUNTERS_BLIND = builder.define("hunters_blind", false);
        MOBS_KNOCKBACK_10000 = builder.define("mobs_knockback_10000", false);
        FALL_DAMAGE_HEALS_SPEEDRUNNER = builder.define("fall_damage_heals_speedrunner",false);
        return null;
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (isSpeedrunner(event.getPlayer())) {
            stop(event.getPlayer().getServer(), true);
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
            stop(player.getServer(), false);
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

    private static ServerPlayerEntity getOtherPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        List<ServerPlayerEntity> otherPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        otherPlayers.removeIf(serverPlayerEntity -> serverPlayerEntity.getGameProfile().getId().equals(player.getGameProfile().getId()));
        return !otherPlayers.isEmpty() ? otherPlayers.get(0) : null;
    }

    public static void stop(MinecraftServer server, boolean abort) {
        //bossInfo.removeAllPlayers();
        if (!abort)
            server.getPlayerList().func_232641_a_(new TranslationTextComponent("text.speedrunnervshunter.speedrunner_win"), ChatType.CHAT, Util.DUMMY_UUID);
        speedrunnerID = null;
    }
}
