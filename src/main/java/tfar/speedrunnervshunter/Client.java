package tfar.speedrunnervshunter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.BeaconTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import java.util.ArrayList;
import java.util.List;

public class Client {

    public static List<BlockPos> trophyPos = new ArrayList<>();

    private static final ResourceLocation TEXTURE_BEACON_BEAM = new ResourceLocation("textures/entity/beacon_beam.png");

    public static void renderLast(RenderWorldLastEvent event) {
        for (BlockPos pos : trophyPos) {
            Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE_BEACON_BEAM);
            BeaconTileEntityRenderer.renderBeamSegment(
                    -TileEntityRendererDispatcher.staticPlayerX + pos.getX(),
                    -TileEntityRendererDispatcher.staticPlayerY + pos.getY(),
                    -TileEntityRendererDispatcher.staticPlayerZ + pos.getZ(),
                    event.getPartialTicks(), 1, Minecraft.getInstance().world.getGameTime(),
                    0, 200,
                    new float[]{1, .9f, 0}, .2f, .25);
        }
    }

    public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        trophyPos.clear();
    }
}
