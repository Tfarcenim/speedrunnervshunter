package tfar.speedrunnervshunter.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import tfar.speedrunnervshunter.SpeedrunnerVsHunter;


public class PacketHandler {

  public static SimpleChannel INSTANCE;

  public static void registerMessages(String channelName) {
    INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(SpeedrunnerVsHunter.MODID, channelName), () -> "1.0", s -> true, s -> true);
    INSTANCE.registerMessage(0, S2CAddTrophyPosPacket.class,
            S2CAddTrophyPosPacket::encode,
            S2CAddTrophyPosPacket::new,
            S2CAddTrophyPosPacket::handle);

    INSTANCE.registerMessage(1, S2CRemoveTrophyPosPacket.class,
            S2CRemoveTrophyPosPacket::encode,
            S2CRemoveTrophyPosPacket::new,
            S2CRemoveTrophyPosPacket::handle);
  }
}