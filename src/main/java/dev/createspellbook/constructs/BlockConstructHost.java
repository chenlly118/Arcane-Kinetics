package dev.createspellbook.constructs;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Both casting blocks share one invisible native caster and menu transport. */
public interface BlockConstructHost extends ConstructHost {
    SmartBlockEntity blockEntity();
    Vec3 muzzle();
    Direction facing();
}
