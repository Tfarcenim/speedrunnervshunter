package tfar.speedrunnervshunter;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ObjectHolder;

public class ModProxy {
    public static final String CRAYFISH_GUNS = "cgm";

    public static final boolean CRAYFISH_GUNS_LOADED = ModList.get().isLoaded("cgm");
    public static final String CRAYFISH_VEHICLES = "vehicle";

    @ObjectHolder(CRAYFISH_GUNS +":"+"pistol")
    public static final Item PISTOL = Items.AIR;

    @ObjectHolder(CRAYFISH_VEHICLES +":"+"vehicle_crate")
    public static final Item VEHICLE_CRATE = Items.AIR;

    @ObjectHolder(CRAYFISH_VEHICLES +":"+"wrench")
    public static final Item WRENCH = Items.AIR;
}
