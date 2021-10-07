package tfar.speedrunnervshunter;

import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import org.apache.commons.lang3.tuple.Pair;
import tfar.speedrunnervshunter.commands.MainCommand;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(SpeedrunnerVsHunter.MODID)
@Mod.EventBusSubscriber
public class SpeedrunnerVsHunter {
    public static final String MODID = "speedrunnervshunter";

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
        MinecraftForge.EVENT_BUS.addListener(this::sneak);
    }

    // private static ServerBossInfo bossInfo;

    public void onServerStart(FMLServerStartingEvent e) {

    }

    public void commands(RegisterCommandsEvent e) {
        MainCommand.registerCommands(e.getDispatcher());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.START && !e.player.level.isClientSide && GameManager.speedrunnerID != null) {
            BlockPos nearest;
            if (GameManager.isSpeedrunner(e.player)) {
                nearest = findNearestTrophy((ServerPlayer) e.player);
            } else {
                if (GameManager.HUNTERS_BLIND.get() && e.player.level.getGameTime() % 20 == 0) {
                    e.player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
                }
                nearest = new BlockPos(e.player.getServer().getPlayerList().getPlayer(GameManager.speedrunnerID).blockPosition());
            }
            ((ServerPlayer) e.player).connection.send(new ClientboundSetDefaultSpawnPositionPacket(nearest, ((ServerPlayer) e.player).getLevel().getSharedSpawnAngle()));
        }
    }

    public static int TIME_LIMIT = 1200;

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (GameManager.speedrunnerID != null && event.phase == TickEvent.Phase.START) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();


            final long scaledTime = server.getLevel(Level.OVERWORLD).getGameTime() % TIME_LIMIT;
            //  bossInfo.setPercent(1 - (scaledTime / (float)TIME_LIMIT));
            if (scaledTime == TIME_LIMIT - 200) {
                final List<ServerPlayer> players = server.getPlayerList().getPlayers().stream().filter(playerMP -> !(playerMP instanceof FakePlayer)).collect(Collectors.toList());
                //if (players.size() > 1) {
                //          server.getPlayerList().broadcastMessage(new TextComponent("Swapping in 10 seconds!"),ChatType.CHAT,Util.DUMMY_UUID);
                //  }
            }

            if (scaledTime == 0 && false) {
                List<ServerPlayer> players = Lists.newArrayList(server.getPlayerList().getPlayers());
                players.removeIf(serverPlayer -> serverPlayer.getGameProfile().getId().equals(GameManager.speedrunnerID));
                if (!players.isEmpty()) {
                    ServerPlayer newSpeedrunner = players.get(GameManager.rand.nextInt(players.size()));
                    UUID oldSpeedrunnerID = GameManager.speedrunnerID;
                    ServerPlayer oldspeedrunner = server.getPlayerList().getPlayer(oldSpeedrunnerID);
                    GameManager.speedrunnerID = newSpeedrunner.getGameProfile().getId();

                    Inventory oldinventory = oldspeedrunner.getInventory();
                    for (ItemStack stack : Stream.of(oldinventory.items, oldinventory.armor, oldinventory.offhand).flatMap(Collection::stream).collect(Collectors.toList())) {
                        if (stack.getItem() == Items.COMPASS) {
                            stack.setHoverName(new TextComponent("Hunter's Compass"));
                        }
                    }

                    Inventory newinventory = newSpeedrunner.getInventory();
                    for (ItemStack stack : Stream.of(newinventory.items, newinventory.armor, newinventory.offhand).flatMap(Collection::stream).collect(Collectors.toList())) {
                        if (stack.getItem() == Items.COMPASS) {
                            stack.setHoverName(new TextComponent("Speedrunner's Compass"));
                        }
                    }
                    TranslatableComponent TranslatableComponent =
                            new TranslatableComponent("commands.speedrunnervshunter.speedrunner.success", newSpeedrunner.getDisplayName());
                    server.getPlayerList().broadcastMessage(TranslatableComponent, ChatType.CHAT, Util.NIL_UUID);
                }
            }
        }
    }

    public static void updateStatus(ServerPlayer playerEntity) {
        if (GameManager.isSpeedrunner(playerEntity)) {
     //       playerEntity.connection.send(new ClientboundSetActionBarTextPacket(new TextComponent("You're the Speedrunner"), 0, TIME_LIMIT, 0));
        } else {
      //      playerEntity.connection.send(new ClientboundSetActionBarTextPacket(new TextComponent("You're a Hunter"), 0, TIME_LIMIT, 0));
        }
    }

    private void sneak(EntityEvent.Size e) {
        Entity entity = e.getEntity();
        if (entity instanceof Player && !entity.level.isClientSide) {
            Player player = (Player) entity;//naturaldisasters
            if (e.getPose() == Pose.CROUCHING && GameManager.isSpeedrunner(player) && ModList.get().isLoaded("naturaldisasters")) {
                Utils.randomDisasterToHunters(player);
            }
        }
    }

    private void knockback(LivingKnockBackEvent event) {
        if (MOBS_KNOCKBACK_10000.get()) {
            LivingEntity target = event.getEntityLiving();
            // DamageSource source = target.getLastDamageSource();
            LivingEntity attacker = target.getLastHurtByMob();
            if (attacker instanceof Mob) {
                event.setStrength(event.getOriginalStrength() + 5000);
            }
        }
    }

    private void fallDamage(LivingHurtEvent e) {
        if (FALL_DAMAGE_HEALS_SPEEDRUNNER.get()) {
            LivingEntity living = e.getEntityLiving();
            if (e.getSource() == DamageSource.FALL && living instanceof Player && GameManager.isSpeedrunner((Player) living)) {
                living.heal(e.getAmount());
                e.setCanceled(true);
            }
        }
    }

    public static ForgeConfigSpec.BooleanValue MOBS_KNOCKBACK_10000;
    public static ForgeConfigSpec.BooleanValue FALL_DAMAGE_HEALS_SPEEDRUNNER;
    public static ForgeConfigSpec.BooleanValue RANDOM_HUNTER_DISASTER;
    //  public static ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_DROPS_1;
    // public static ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_DROPS_2;


    private SpeedrunnerVsHunter spec(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        GameManager.HUNTERS_BLIND = builder.define("hunters_blind", false);
        MOBS_KNOCKBACK_10000 = builder.define("mobs_knockback_10000", false);
        FALL_DAMAGE_HEALS_SPEEDRUNNER = builder.define("fall_damage_heals_speedrunner", false);
        RANDOM_HUNTER_DISASTER = builder.define("random_hunter_disaster", false);
        //  ITEM_DROPS_1 = builder.defineList("item_drops_1",defaultGuns(),String.class::isInstance);
        //  ITEM_DROPS_2 = builder.defineList("item_drops_2",defaultAmmo(),String.class::isInstance);

        return null;
    }

    //  public static List<String> defaultGuns() {
    //      List<String> guns = Lists.newArrayList(ModItems.PISTOL,ModItems.SHOTGUN,ModItems.MACHINE_PISTOL,ModItems.ASSAULT_RIFLE,ModItems.BAZOOKA).stream()
    //              .map(RegistryObject::getId).map(ResourceLocation::toString).collect(Collectors.toList());
    //      return guns;
    //   }

    // public static List<String> defaultAmmo() {
    //       List<String> guns = Lists.newArrayList(ModItems.BASIC_BULLET,ModItems.SHELL,ModItems.BASIC_BULLET,ModItems.BASIC_BULLET,ModItems.MISSILE)
    //              .stream().map(registryObject -> registryObject.getId()).map(resourceLocation -> resourceLocation.toString()).collect(Collectors.toList());
    //     return guns;
    // }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (GameManager.isSpeedrunner(event.getPlayer())) {
            GameManager.stop(event.getPlayer().getServer(), Resolution.EXIT);
        }
    }

    public static int INDEX = 0;

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();
        Player player = event.getPlayer();
        for (Iterator<TrophyLocation> iterator = GameManager.TROPHY_LOCATIONS.iterator(); iterator.hasNext(); ) {
            TrophyLocation trophyLocation = iterator.next();
            if (trophyLocation.getPos().equals(pos)) {
                player.sendMessage(new TranslatableComponent("text.speedrunnervshunter.trophy_get"), Util.NIL_UUID);
                iterator.remove();
                handleSpecialDrops(player);
            }
        }

        if (GameManager.speedrunnerID != null) {
            if (GameManager.TROPHY_LOCATIONS.isEmpty()) {
                GameManager.stop(player.getServer(), Resolution.WIN);
            } else {
                GameManager.placeNextTrophy(player.getServer());
            }
        }
    }

    private static void handleSpecialDrops(Player player) {
        List<ItemStack> vehicles = vehicles();
        if (vehicles.size() > INDEX) {

            ItemStack fuelStack = new ItemStack(
                    Registry.ITEM.get(new ResourceLocation(ModProxy.CRAYFISH_VEHICLES, "industrial_jerry_can")));

            fuelStack.getOrCreateTag().putInt("Fuel", 15000);

            player.addItem(vehicles.get(INDEX));
            player.addItem(fuelStack);
        }
        INDEX++;
    }

    public static List<ItemStack> vehicles() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(makeCrate("vehicle:bumper_car"));
        stacks.add(makeCrate("vehicle:moped"));
        stacks.add(makeCrate("vehicle:smart_car"));
        stacks.add(makeCrate("vehicle:mini_bike"));
        stacks.add(makeCrate("vehicle:go_kart"));
        stacks.add(makeCrate("vehicle:sports_plane"));

        return stacks;
    }

    public static ItemStack makeCrate(String vehicle) {
        ItemStack stack = new ItemStack(ModProxy.VEHICLE_CRATE);
        CompoundTag tag = new CompoundTag();
        tag.putString("Vehicle", vehicle);
        tag.putInt("WheelType", 0);
        tag.putInt("EngineTier", 0);
        stack.getOrCreateTag().put("BlockEntityTag", tag);
        return stack;
    }

    private static BlockPos findNearestTrophy(ServerPlayer playerMP) {
        double dist = Double.MAX_VALUE;
        BlockPos near = GameManager.activeBlock;//null;
        /*
        BlockPos playerPos = playerMP.blockPosition();
        for (TrophyLocation pos : GameManager.TROPHY_LOCATIONS) {
            double dist1 = playerPos.distSqr(pos.getPos());
            if (dist1 < dist) {
                dist = dist1;
                near = pos.getPos();
            }
        }*/

        return near;
    }

    private static ServerPlayer getOtherPlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        List<ServerPlayer> otherPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        otherPlayers.removeIf(serverPlayer -> serverPlayer.getGameProfile().getId().equals(player.getGameProfile().getId()));
        return !otherPlayers.isEmpty() ? otherPlayers.get(0) : null;
    }

}
