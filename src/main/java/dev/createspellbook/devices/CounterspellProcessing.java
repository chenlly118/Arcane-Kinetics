package dev.createspellbook.devices;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** Counterspell has no damage/block-impact event; called after its real onCast returns. */
public final class CounterspellProcessing {
    public static void complete(Level world,int spellLevel,LivingEntity caster){
        if(!(world instanceof ServerLevel level) || spellLevel<1)return;
        if(caster instanceof dev.createspellbook.constructs.ConstructCaster machine && dev.createspellbook.constructs.ConstructSafety.sealedCaster(machine))return;
        var spell=SpellRegistry.getSpell("irons_spellbooks:counterspell");var start=caster.getEyePosition();
        var contact=new SpellInteraction(spell.getSpellResource(),spell.getSchoolType().getId(),spellLevel,0,caster,caster.getUUID(),UUID.randomUUID(),null);
        // These are the installed 3.16.3 Counterspell's own public-raycast
        // parameters, verified against its onCast: 80 blocks, 0.35 inflation.
        var hit=RaycastBuilder.begin(level,caster).start(start).end(start.add(caster.getForward().normalize().scale(80)))
                .checkForBlocks(true).bbInflation(.35f)
                .filter(e->e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity || e instanceof ItemEntity item && matching(level,item.getItem(),contact).isPresent())).build();
        if(hit instanceof EntityHitResult entityHit){
            if(entityHit.getEntity() instanceof SpellDeviceProbe probe && probe.device()!=null)probe.device().acceptSpell(contact);
            else if(entityHit.getEntity() instanceof ItemEntity item)processItem(level,item,contact);
        }else if(hit instanceof BlockHitResult block && level.hasChunkAt(block.getBlockPos()) && SpellTarget.at(level,block.getBlockPos())!=null)SpellTarget.at(level,block.getBlockPos()).acceptSpell(contact);
    }
    private static void processItem(ServerLevel level,ItemEntity item,SpellInteraction contact){
        var original=item.getItem().copy();
        var match=matching(level,original,contact);
        if(match.isEmpty())return;
        var recipe=match.get().value();var output=recipe.result().copy();
        if(output.isEmpty() || output.getCount()>output.getMaxStackSize())return;
        var remaining=original.copy();remaining.shrink(recipe.inputCount());
        if(remaining.isEmpty()){
            var fresh=item.saveWithoutId(new CompoundTag());fresh.putShort("Age",(short)0);item.load(fresh);item.setItem(output);
        }
        else{
            // Native NBT preserves pickup/ownership and motion. Use a fresh UUID
            // and age for the output; restore the input if insertion is canceled.
            var saved=item.saveWithoutId(new CompoundTag());saved.remove("UUID");saved.putShort("Age",(short)0);
            var result=new ItemEntity(level,item.getX(),item.getY(),item.getZ(),output);result.load(saved);result.setItem(output);
            item.setItem(remaining);
            if(!level.addFreshEntity(result)){item.setItem(original);return;}
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,item.getX(),item.getY()+.2,item.getZ(),8,.15,.15,.15,.02);
    }
    private static java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<ArcaneProcessingRecipe>> matching(ServerLevel level,net.minecraft.world.item.ItemStack input,SpellInteraction contact){
        // Produced stabilizers must not intercept later casts aimed at the
        // remaining stack. Ordinary non-processable drops are not spell targets.
        return level.getRecipeManager().getAllRecipesFor(DeviceContent.ARCANE_PROCESSING.get()).stream()
                .filter(r->r.value().accepts(input,contact.spellId(),contact.schoolId(),contact.level()))
                .sorted(java.util.Comparator.comparing(r->r.id().toString())).findFirst();
    }
    private CounterspellProcessing(){}
}
