package tfar.speedrunnervshunter;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.List;
import java.util.function.Supplier;

public class GauntletEvents {

    public static final String GAUNTLETMODID = "igauntlet";

    private static int index = 0;

    @GameRegistry.ObjectHolder(GAUNTLETMODID +":mind_stone")
    public static Item mind_stone;

    @GameRegistry.ObjectHolder(GAUNTLETMODID +":power_stone")
    public static Item power_stone;

    @GameRegistry.ObjectHolder(GAUNTLETMODID +":reality_stone")
    public static Item reality_stone;

    @GameRegistry.ObjectHolder(GAUNTLETMODID +":soul_stone")
    public static Item soul_stone;

    @GameRegistry.ObjectHolder(GAUNTLETMODID +":space_stone")
    public static Item space_stone;

    @GameRegistry.ObjectHolder(GAUNTLETMODID +":time_stone")
    public static Item time_stone;

    public static final List<Supplier<Item>> items = Lists.newArrayList(() -> mind_stone,() -> power_stone, () -> reality_stone,
            () -> soul_stone,() -> space_stone, () -> time_stone);

    public static void giveStone(EntityPlayer player) {
        player.addItemStackToInventory(new ItemStack(items.get(index).get()));
        index++;
        if (index >= items.size()) index = 0;
    }
}
