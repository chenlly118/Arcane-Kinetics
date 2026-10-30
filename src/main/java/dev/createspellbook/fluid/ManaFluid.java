package dev.createspellbook.fluid;

import com.simibubi.create.content.fluids.VirtualFluid;
import dev.createspellbook.Content;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Create's virtual fluid has an AIR legacy block: transportable, never world-placeable. */
public final class ManaFluid extends VirtualFluid {
    public ManaFluid() {
        super(new BaseFlowingFluid.Properties(Content.MANA_TYPE,()->Content.MANA.get(),()->Content.MANA.get()),true);
    }
    @Override public Item getBucket() { return Content.MANA_BUCKET.get(); }
}
