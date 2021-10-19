package tfar.speedrunnervshunter.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Shadow
    @Final
    private List<NonNullList<ItemStack>> compartments;

    @Shadow
    @Final
    public Player player;

    @Inject(method = "dropAll", at = @At("HEAD"), cancellable = true)
    private void dontDropCompass(CallbackInfo ci) {
        for (List<ItemStack> list : this.compartments) {
            for (int i = 0; i < list.size(); ++i) {
                ItemStack itemstack = list.get(i);
                if (!itemstack.isEmpty() && itemstack.getItem() != Items.COMPASS) {
                    this.player.drop(itemstack, true, false);
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }
        ci.cancel();
    }
}
