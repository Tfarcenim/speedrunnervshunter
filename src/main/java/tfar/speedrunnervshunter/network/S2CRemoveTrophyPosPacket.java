package tfar.speedrunnervshunter.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import tfar.speedrunnervshunter.Client;

import java.util.function.Supplier;

public class S2CRemoveTrophyPosPacket {

  BlockPos rec;

  public S2CRemoveTrophyPosPacket() {
  }

  public S2CRemoveTrophyPosPacket(BlockPos toSend) {
    rec = toSend;
  }

  public S2CRemoveTrophyPosPacket(PacketBuffer buf) {
    rec = BlockPos.fromLong(buf.readLong());
  }

  public void encode(PacketBuffer buf) {
    buf.writeLong(rec.toLong());
  }

  public void handle(Supplier<Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Client.trophyPos.remove(rec);
    });
    ctx.get().setPacketHandled(true);
  }
}