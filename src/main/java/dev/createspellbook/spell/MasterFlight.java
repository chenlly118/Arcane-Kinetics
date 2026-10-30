package dev.createspellbook.spell;

import net.minecraft.world.phys.Vec3;

/** The synchronized direction is a full origin-to-impact vector, not just a yaw. */
public final class MasterFlight {
    public static final int IMPACT_TICKS=6;
    public static Vec3 offset(Vec3 route,float age,boolean saw) {
        double t=Math.clamp(age/IMPACT_TICKS,0,1);
        // The saw cuts through the impact point between its two damage segments.
        if(saw && age>IMPACT_TICKS) t+=Math.clamp((age-IMPACT_TICKS)/4,0,1)*.28;
        return route.scale(t);
    }
    private MasterFlight() {}
}
