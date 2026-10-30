package dev.createspellbook.item;

import dev.createspellbook.Content;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import java.util.List;

/** Container for Iron's existing no-world blood, without changing its fluid or vanilla buckets. */
public final class BloodBucketItem extends Item {
    public BloodBucketItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tooltip.create_spellbook.blood_bucket"));
    }

    /** Native full-bucket drain/simulation semantics; never manufactures a second blood fluid. */
    public static final class Handler extends FluidBucketWrapper {
        public Handler(ItemStack stack) { super(stack); }
        @Override public FluidStack getFluid() {
            return container.is(Content.BLOOD_BUCKET.get())?new FluidStack(FluidRegistry.BLOOD.get(),1000):FluidStack.EMPTY;
        }
        @Override public boolean canFillFluidType(FluidStack fluid) { return false; }
        @Override public boolean isFluidValid(int tank,FluidStack fluid) { return fluid.is(FluidRegistry.BLOOD.get()); }
    }
}
