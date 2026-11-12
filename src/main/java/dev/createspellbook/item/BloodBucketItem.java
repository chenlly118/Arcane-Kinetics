package dev.createspellbook.item;

import dev.createspellbook.Content;
import io.redspace.ironsspellbooks.registries.FluidRegistry;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

/** A non-pouring container for the active blood fluid. */
public final class BloodBucketItem extends Item {
    public BloodBucketItem(Properties properties) { super(properties); }

    /** Full-bucket drain and simulation semantics. */
    public static final class Handler extends FluidBucketWrapper {
        public Handler(ItemStack stack) { super(stack); }
        @Override public FluidStack getFluid() {
            return container.is(Content.BLOOD_BUCKET.get())?new FluidStack(FluidRegistry.BLOOD.get(),1000):FluidStack.EMPTY;
        }
        @Override public boolean canFillFluidType(FluidStack fluid) { return false; }
        @Override public boolean isFluidValid(int tank,FluidStack fluid) { return fluid.is(FluidRegistry.BLOOD.get()); }
    }
}
