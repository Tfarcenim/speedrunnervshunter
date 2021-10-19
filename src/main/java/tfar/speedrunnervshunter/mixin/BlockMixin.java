package tfar.speedrunnervshunter.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(BlockBehaviour.class)
public class BlockMixin {
    @Inject(method = "getDrops",at = @At("HEAD"),cancellable =true)
    private void intercept(BlockState p_60537_, LootContext.Builder p_60538_, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (p_60537_.getBlock() == Blocks.GOLD_BLOCK) {
            cir.setReturnValue(Collections.emptyList());
        }
    }
}
