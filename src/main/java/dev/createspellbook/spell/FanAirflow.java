package dev.createspellbook.spell;

import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * Reuse Create's real blasting air-flow description instead of inventing a colour.
 *
 * {@code AllFanProcessingTypes.BLASTING.morphAirFlow} supplies the exact lava
 * gradient (0xFF4400..0xFF8855), its 0.5 alpha and the rare FLAME / lava block
 * extra particles.  Create's own {@code create:air_flow} particle cannot be sent
 * from a server stand-alone: it resolves a live fan source at the encoded block
 * position and discards itself without one.  The access implementation below is
 * therefore a thin adapter onto vanilla particle packets; the visual parameters
 * stay owned by the installed Create version.
 */
public final class FanAirflow {
    public static void spawnBlasting(ServerLevel level, Vec3 pos) {
        List<ParticleOptions> extras = new ArrayList<>();
        int[] color = {0xFF6600};
        AllFanProcessingTypes.BLASTING.morphAirFlow(new FanProcessingType.AirFlowParticleAccess() {
            @Override public void setColor(int rgb) { color[0] = rgb; }
            @Override public void setAlpha(float alpha) { /* dust particles carry no alpha; scale is softened below */ }
            @Override public void spawnExtraParticle(ParticleOptions options, float size) { extras.add(options); }
        }, level.random);
        int rgb = color[0];
        // Slightly smaller dust approximates the translucent air-flow puffs.
        Vector3f tint = new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        level.sendParticles(new DustParticleOptions(tint, .8f), pos.x, pos.y, pos.z, 9, .45, .45, .45, 0);
        for (ParticleOptions extra : extras)
            level.sendParticles(extra, pos.x, pos.y, pos.z, 3, .35, .35, .35, .01);
    }
    private FanAirflow() {}
}
